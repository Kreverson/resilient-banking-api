package com.resilientbanking.infra.execution;

import java.util.concurrent.CompletableFuture;

/**
 * Um pedido esperando uma thread da cozinha.
 *
 * @param arrivalNanos instante em que o pedido chegou na porta (System.nanoTime)
 * @param task         o restante da cadeia de filtros e o controller
 * @param done         completado pela worker quando o pedido termina
 */
public record WorkItem(RequestPriority priority, long arrivalNanos, Runnable task, CompletableFuture<Void> done) {
}
