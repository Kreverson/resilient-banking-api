package com.resilientbanking.infra.execution;

/** O pedido ficou na fila mais tempo do que o cliente espera; foi descartado sem processar. */
public class RequestExpiredException extends RuntimeException {

    public RequestExpiredException() {
        super("Pedido expirou na fila", null, false, false);
    }
}
