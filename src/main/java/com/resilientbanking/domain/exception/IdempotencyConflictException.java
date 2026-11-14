package com.resilientbanking.domain.exception;

public class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("A chave de idempotencia " + idempotencyKey + " ja foi usada com outro conteudo");
    }
}
