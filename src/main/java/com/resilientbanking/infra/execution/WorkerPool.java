package com.resilientbanking.infra.execution;

import com.resilientbanking.infra.config.BankProperties;
import com.resilientbanking.infra.observability.ResilienceMetrics;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A "cozinha": um numero fixo de threads que tiram pedidos de uma fila.
 *
 * Branch 04 (load shedding por prioridade): uma fila so, FIFO, com um limite diferente para cada prioridade.
 * O pre-carregamento so entra com a fila abaixo de 20, o extrato ate 100 e a transacao ate 150. Assim a recusa
 * comeca pelo que ninguem esta olhando e sempre sobra lugar para o pedido critico.
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final BlockingDeque<WorkItem> queue;
    private final AtomicInteger busy = new AtomicInteger();
    private final List<Thread> workers = new ArrayList<>();
    private final Map<RequestPriority, Integer> queueLimits;

    public WorkerPool(BankProperties properties, ResilienceMetrics metrics) {
        this.queueLimits = new EnumMap<>(properties.queueLimits());
        this.queue = new LinkedBlockingDeque<>(Collections.max(queueLimits.values()));
        metrics.bindPool(queue::size, busy::get, properties.workers());
        for (int i = 1; i <= properties.workers(); i++) {
            Thread worker = Thread.ofPlatform().name("worker-" + i).daemon().start(this::loop);
            workers.add(worker);
        }
    }

    /** @return false quando a fila passou do limite da prioridade do pedido e ele deve ser recusado */
    public boolean submit(WorkItem item) {
        if (queue.size() >= queueLimits.get(item.priority())) {
            return false;
        }
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
