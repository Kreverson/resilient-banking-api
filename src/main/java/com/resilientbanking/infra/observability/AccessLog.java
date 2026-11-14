package com.resilientbanking.infra.observability;

import com.resilientbanking.infra.config.BankProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Uma linha estruturada por pedido, com os mesmos campos em todas as branches. */
@Component
public class AccessLog {

    private static final Logger log = LoggerFactory.getLogger("access");

    private final boolean enabled;

    public AccessLog(BankProperties properties) {
        this.enabled = properties.accessLog();
    }

    /** requestId e priority ja estao no MDC e saem em todas as linhas de log do pedido. */
    public void log(RequestTrace trace, Outcome outcome) {
        if (!enabled) {
            return;
        }
        log.atInfo()
                .addKeyValue("method", trace.method())
                .addKeyValue("path", trace.path())
                .addKeyValue("status", trace.status())
                .addKeyValue("outcome", outcome.tag())
                .addKeyValue("queueMs", millis(trace.queueNanos()))
                .addKeyValue("serviceMs", millis(trace.serviceNanos()))
                .addKeyValue("totalMs", millis(trace.totalNanos()))
                .log("{} {} {} {}", trace.method(), trace.path(), trace.status(), outcome.tag());
    }

    private static long millis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(nanos, 0));
    }
}
