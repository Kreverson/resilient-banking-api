package com.resilientbanking.domain.port;

import com.resilientbanking.domain.model.Operation;

/**
 * Custo de processamento de cada etapa (antifraude, montagem do extrato, insights, comprovantes).
 * Na infra ele e simulado com tempo fixo para que a capacidade da API seja previsivel nos testes.
 */
public interface ProcessingCost {

    void spend(Operation operation);
}
