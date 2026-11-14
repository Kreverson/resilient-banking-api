package com.resilientbanking.infra.simulation;

import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.domain.port.ProcessingCost;
import com.resilientbanking.infra.config.BankProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Segura a thread pelo tempo configurado, como fariam o antifraude, a montagem do extrato ou o motor de
 * recomendacao. O que importa no experimento e que cada pedido ocupa uma thread da cozinha por um tempo conhecido.
 */
@Component
public class SimulatedProcessingCost implements ProcessingCost {

    private final BankProperties properties;

    public SimulatedProcessingCost(BankProperties properties) {
        this.properties = properties;
    }

    @Override
    public void spend(Operation operation) {
        Duration duration = properties.cost().getOrDefault(operation, Duration.ZERO);
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        }
    }
}
