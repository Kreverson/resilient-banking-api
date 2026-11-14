package com.resilientbanking.domain.model;

/** Comprovante que o app pre-carrega em segundo plano. */
public record Receipt(Transaction transaction, String authenticationCode) {
}
