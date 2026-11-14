package com.resilientbanking.infra.observability;

import java.util.Locale;

/**
 * Destino final de cada pedido. E a mesma lista em todas as branches para que os paineis sejam comparaveis.
 */
public enum Outcome {
    /** Processado e respondido antes de o cliente desistir. */
    ON_TIME,
    /** Processado, mas depois do timeout do cliente: trabalho jogado fora. */
    LATE,
    /** Recusado na porta (503) sem ocupar worker. */
    REJECTED,
    /** Tirado da fila sem processar porque o cliente ja tinha desistido. */
    DISCARDED;

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
