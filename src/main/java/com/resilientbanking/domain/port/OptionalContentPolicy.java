package com.resilientbanking.domain.port;

/**
 * Decide, pedido a pedido, se a parte opcional da resposta (os insights do extrato) deve ser montada.
 * Sob carga a infra passa a responder "nao" para uma fracao dos pedidos: a resposta fica mais barata em vez de
 * ser recusada.
 */
public interface OptionalContentPolicy {

    boolean serveOptionalContent();
}
