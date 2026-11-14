package com.resilientbanking.infra.execution;

import com.resilientbanking.infra.config.BankProperties;
import com.resilientbanking.infra.observability.ResilienceMetrics;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A "cozinha": um numero fixo de threads que tiram pedidos de uma fila.
 *
 * Branch 02 (load shedding): a fila tem um numero fixo de lugares. Com a fila cheia o pedido nem entra:
 * a porta devolve 503 na hora, sem ocupar uma worker. Com 100 lugares e a cozinha soltando ~350 pedidos/s,
 * a espera maxima fica abaixo de 0,3s, entao todo pedido que entra ainda tem cliente esperando.
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final BlockingDeque<WorkItem> queue;
    private final AtomicInteger busy = new AtomicInteger();
    private final List<Thread> workers = new ArrayList<>();

    public WorkerPool(BankProperties properties, ResilienceMetrics metrics) {
        this.queue = new LinkedBlockingDeque<>(properties.queueCapacity());
        metrics.bindPool(queue::size, busy::get, properties.workers());
        for (int i = 1; i <= properties.workers(); i++) {
            Thread worker = Thread.ofPlatform().name("worker-" + i).daemon().start(this::loop);
            workers.add(worker);
        }
    }

    /** @return false quando a fila esta cheia e o pedido deve ser recusado */
    public boolean submit(WorkItem item) {
        return queue.offerLast(item);
    }

    private void loop() {
        while (!Thread.currentThread().isInterrupted()) {
            WorkItem item;
            try {
                item = queue.takeFirst();
            } catch (InterruptedException e) {
                return;
            }
            busy.incrementAndGet();
            try {
                item.task().run();
                item.done().complete(null);
            } catch (Throwable t) {
                item.done().completeExceptionally(t);
            } finally {
                busy.decrementAndGet();
            }
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("Parando {} workers com {} pedidos na fila", workers.size(), queue.size());
        workers.forEach(Thread::interrupt);
    }
}
