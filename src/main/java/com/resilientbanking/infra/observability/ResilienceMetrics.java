package com.resilientbanking.infra.observability;

import com.resilientbanking.infra.execution.RequestPriority;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

/**
 * Metricas comuns a todas as branches. Todas sao registradas na main, mesmo as que so as branches usam
 * (recusa, descarte, dimmer), para que o mesmo painel do Grafana funcione em qualquer branch.
 *
 * <ul>
 *   <li>bank_requests_received_total{priority}: pedidos que chegaram na porta</li>
 *   <li>bank_requests_finished_total{priority,outcome,status}: destino de cada pedido; goodput e
 *       outcome=on_time com status 2xx</li>
 *   <li>bank_queue_wait_seconds{priority}: tempo esperando uma worker</li>
 *   <li>bank_service_time_seconds{priority}: tempo ocupando uma worker</li>
 *   <li>bank_queue_size, bank_workers_busy, bank_workers_total: estado da cozinha</li>
 *   <li>bank_brownout_dimmer: fracao de extratos que saem com insights (1 = sem degradacao)</li>
 *   <li>bank_statement_degraded_total: extratos que sairam sem insights</li>
 * </ul>
 */
@Component
public class ResilienceMetrics {

    private final MeterRegistry registry;
    private volatile double dimmer = 1.0;

    public ResilienceMetrics(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("bank.brownout.dimmer", () -> dimmer)
                .description("Fracao de extratos com insights personalizados")
                .register(registry);
        Counter.builder("bank.statement.degraded").register(registry);
        for (RequestPriority priority : RequestPriority.values()) {
            Counter.builder("bank.requests.received").tag("priority", priority.tag()).register(registry);
            queueWait(priority);
            serviceTime(priority);
        }
    }

    public void bindPool(IntSupplier queueSize, IntSupplier busyWorkers, int totalWorkers) {
        Gauge.builder("bank.queue.size", queueSize::getAsInt).register(registry);
        Gauge.builder("bank.workers.busy", busyWorkers::getAsInt).register(registry);
        Gauge.builder("bank.workers.total", () -> totalWorkers).register(registry);
    }

    public void received(RequestPriority priority) {
        registry.counter("bank.requests.received", "priority", priority.tag()).increment();
    }

    public void finished(RequestTrace trace, Outcome outcome) {
        registry.counter("bank.requests.finished",
                "priority", trace.priority().tag(),
                "outcome", outcome.tag(),
                "status", statusClass(trace.status())).increment();
        queueWait(trace.priority()).record(trace.queueNanos(), TimeUnit.NANOSECONDS);
        if (outcome == Outcome.ON_TIME || outcome == Outcome.LATE) {
            serviceTime(trace.priority()).record(trace.serviceNanos(), TimeUnit.NANOSECONDS);
        }
    }

    public void statementDegraded() {
        registry.counter("bank.statement.degraded").increment();
    }

    public void dimmer(double value) {
        this.dimmer = value;
    }

    private Timer queueWait(RequestPriority priority) {
        return Timer.builder("bank.queue.wait")
                .tag("priority", priority.tag())
                .publishPercentileHistogram()
                .maximumExpectedValue(Duration.ofSeconds(30))
                .register(registry);
    }

    private Timer serviceTime(RequestPriority priority) {
        return Timer.builder("bank.service.time")
                .tag("priority", priority.tag())
                .publishPercentileHistogram()
                .maximumExpectedValue(Duration.ofSeconds(5))
                .register(registry);
    }

    private static String statusClass(int status) {
        return (status / 100) + "xx";
    }
}
