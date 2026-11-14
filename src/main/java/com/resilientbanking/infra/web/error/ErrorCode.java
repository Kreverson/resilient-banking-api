package com.resilientbanking.infra.web.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Requisicao invalida"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Corpo da requisicao ilegivel"),
    INVALID_TRANSACTION(HttpStatus.UNPROCESSABLE_CONTENT, "Transacao invalida"),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_CONTENT, "Saldo insuficiente"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Conta nao encontrada"),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "Conflito de idempotencia"),
    CONCURRENT_REQUEST(HttpStatus.CONFLICT, "Requisicao concorrente com a mesma chave"),
    SERVICE_OVERLOADED(HttpStatus.SERVICE_UNAVAILABLE, "Servico sobrecarregado"),
    REQUEST_EXPIRED(HttpStatus.SERVICE_UNAVAILABLE, "Pedido expirado na fila"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
