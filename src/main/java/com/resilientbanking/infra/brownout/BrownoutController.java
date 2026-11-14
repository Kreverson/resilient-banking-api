package com.resilientbanking.infra.brownout;

import com.resilientbanking.domain.port.OptionalContentPolicy;
import com.resilientbanking.infra.config.BankProperties;
import com.resilientbanking.infra.execution.WorkerPool;
import com.resilientbanking.infra.observability.ResilienceMetrics;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Brownout (Klein et al., 2014): a aplicacao ganha um "dimmer", um numero entre 0 e 1 que diz a chance de cada
 * extrato sair com os insights personalizados.
 *
 * A cada intervalo o controlador olha a fila. Passou do limite alto: o dimmer desce rapido. Fila esvaziou (abaixo do
 * limite baixo): ele sobe devagar, para nao ficar ligando e desligando. Entre os dois, fica onde esta.
 */
@Component
public class BrownoutController implements OptionalContentPolicy {

    private final WorkerPool pool;
    private final ResilienceMetrics metrics;
    private final BankProperties.Brownout config;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("brownout").daemon().factory());
    private volatile double dimmer = 1.0;

    public BrownoutController(WorkerPool pool, ResilienceMetrics metrics, BankProperties properties) {
        this.pool = pool;
        this.metrics = metrics;
        this.config = properties.brownout();
        long interval = config.interval().toMillis();
        scheduler.scheduleAtFixedRate(this::adjust, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean serveOptionalContent() {
        if (ThreadLocalRandom.current().nextDouble() < dimmer) {
            return true;
        }
        metrics.statementDegraded();
        return false;
    }

    private void adjust() {
        int queue = pool.queueSize();
        if (queue > config.queueHigh()) {
            dimmer = Math.max(0.0, dimmer - config.stepDown());
        } else if (queue < config.queueLow()) {
            dimmer = Math.min(1.0, dimmer + config.stepUp());
        }
        metrics.dimmer(dimmer);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }
}
