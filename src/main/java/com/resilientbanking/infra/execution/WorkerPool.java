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
 * Na main a fila nao tem limite e anda por ordem de chegada (FIFO). Nao ha protecao nenhuma:
 * quando chega mais do que as workers dao conta, a fila cresce sem parar.
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final BlockingDeque<WorkItem> queue = new LinkedBlockingDeque<>();
    private final AtomicInteger busy = new AtomicInteger();
    private final List<Thread> workers = new ArrayList<>();

    public WorkerPool(BankProperties properties, ResilienceMetrics metrics) {
        metrics.bindPool(queue::size, busy::get, properties.workers());
        for (int i = 1; i <= properties.workers(); i++) {
            Thread worker = Thread.ofPlatform().name("worker-" + i).daemon().start(this::loop);
            workers.add(worker);
        }
    }

    public void submit(WorkItem item) {
        queue.offerLast(item);
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
