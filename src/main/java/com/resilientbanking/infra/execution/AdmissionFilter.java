package com.resilientbanking.infra.execution;

import com.resilientbanking.infra.config.BankProperties;
import com.resilientbanking.infra.observability.AccessLog;
import com.resilientbanking.infra.observability.Outcome;
import com.resilientbanking.infra.observability.RequestTrace;
import com.resilientbanking.infra.observability.ResilienceMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * A "porta": todo pedido de negocio passa por aqui antes de chegar no controller.
 *
 * A thread do Tomcat (virtual) so recebe a conexao, coloca o pedido na fila da cozinha e espera.
 * O resto da cadeia (leitura do corpo, controller, caso de uso) roda numa das workers do {@link WorkerPool}.
 * E aqui que as branches de resiliencia decidem recusar antes de ocupar uma worker.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AdmissionFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final WorkerPool pool;
    private final PriorityClassifier classifier;
    private final ResilienceMetrics metrics;
    private final AccessLog accessLog;
    private final long clientTimeoutNanos;

    public AdmissionFilter(WorkerPool pool, PriorityClassifier classifier, ResilienceMetrics metrics,
                           AccessLog accessLog, BankProperties properties) {
        this.pool = pool;
        this.classifier = classifier;
        this.metrics = metrics;
        this.accessLog = accessLog;
        this.clientTimeoutNanos = properties.clientTimeout().toNanos();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Health check e scrape do Prometheus nunca entram na fila.
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = requestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        RequestTrace trace = new RequestTrace(requestId, request.getMethod(), request.getRequestURI(),
                classifier.classify(request), System.nanoTime());
        metrics.received(trace.priority());

        CompletableFuture<Void> done = new CompletableFuture<>();
        pool.submit(new WorkItem(trace.priority(), trace.arrivalNanos(),
                () -> process(trace, request, response, chain), done));
        await(done);
    }

    private void process(RequestTrace trace, HttpServletRequest request, HttpServletResponse response,
                         FilterChain chain) {
        trace.started(System.nanoTime());
        MDC.put("requestId", trace.requestId());
        MDC.put("priority", trace.priority().tag());
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException e) {
            throw new IllegalStateException(e);
        } finally {
            trace.finished(System.nanoTime(), response.getStatus());
            Outcome outcome = trace.totalNanos() <= clientTimeoutNanos ? Outcome.ON_TIME : Outcome.LATE;
            metrics.finished(trace, outcome);
            accessLog.log(trace, outcome);
            MDC.clear();
        }
    }

    private static void await(CompletableFuture<Void> done) throws ServletException, IOException {
        try {
            done.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() instanceof IllegalStateException ise && ise.getCause() != null
                    ? ise.getCause() : e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof ServletException se) {
                throw se;
            }
            throw new ServletException(cause);
        }
    }

    private static String requestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        return header == null || header.isBlank() || header.length() > 64 ? UUID.randomUUID().toString() : header;
    }
}
