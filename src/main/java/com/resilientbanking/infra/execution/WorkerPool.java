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
 * Branch 03 (LIFO adaptativo + CoDel, como no Facebook):
 * <ul>
 *   <li>LIFO adaptativo: enquanto a fila esta pequena ela anda por ordem de chegada. Quando passa do limite,
 *       a worker pega o pedido mais novo, que e o que ainda tem alguem esperando.</li>
 *   <li>CoDel (controlled delay): se a fila nao ficou vazia nos ultimos 100ms, o sistema esta sobrecarregado e
 *       o tempo maximo de espera cai de 100ms para 5ms. Quem esperou mais que isso e descartado sem processar.</li>
 * </ul>
 * A fila continua sem limite de tamanho.
 */
@Component
public class WorkerPool {

    private static final Logger log = LoggerFactory.getLogger(WorkerPool.class);

    private final BlockingDeque<WorkItem> queue = new LinkedBlockingDeque<>();
    private final AtomicInteger busy = new AtomicInteger();
    private final List<Thread> workers = new ArrayList<>();
    private final BankProperties.Lifo lifo;
    private final BankProperties.Codel codel;
    private volatile long lastEmptyNanos = System.nanoTime();

    public WorkerPool(BankProperties properties, ResilienceMetrics metrics) {
        this.lifo = properties.lifo();
        this.codel = properties.codel();
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
                item = take();
            } catch (InterruptedException e) {
                return;
            }
            if (codel.enabled() && waited(item, System.nanoTime()) > codelLimit(System.nanoTime())) {
                expire(item);
                continue;
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

    private WorkItem take() throws InterruptedException {
        if (codel.enabled()) {
            dropExpiredFromHead();
        }
        WorkItem item = lifo.enabled() && queue.size() > lifo.threshold() ? queue.takeLast() : queue.takeFirst();
        if (queue.isEmpty()) {
            lastEmptyNanos = System.nanoTime();
        }
        return item;
    }

    /** No LIFO os mais antigos ficam no inicio da fila; o CoDel os descarta de la sem esperar a vez deles. */
    private void dropExpiredFromHead() {
        long now = System.nanoTime();
        long limit = codelLimit(now);
        WorkItem head;
        while ((head = queue.peekFirst()) != null && waited(head, now) > limit) {
            WorkItem expired = queue.pollFirst();
            if (expired != null) {
                expire(expired);
            }
        }
    }

    private long codelLimit(long now) {
        boolean overloaded = now - lastEmptyNanos > codel.interval().toNanos();
        return overloaded ? codel.target().toNanos() : codel.interval().toNanos();
    }

    private static long waited(WorkItem item, long now) {
        return now - item.arrivalNanos();
    }

    private static void expire(WorkItem item) {
        item.done().completeExceptionally(new RequestExpiredException());
    }

    @PreDestroy
    void shutdown() {
        log.info("Parando {} workers com {} pedidos na fila", workers.size(), queue.size());
        workers.forEach(Thread::interrupt);
    }
}
