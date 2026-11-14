package com.resilientbanking.domain.model;

/** Etapas com custo de processamento relevante, usadas para simular a carga de cada tipo de pedido. */
public enum Operation {
    FRAUD_CHECK,
    STATEMENT,
    INSIGHTS,
    RECEIPTS,
    BATCH
}
