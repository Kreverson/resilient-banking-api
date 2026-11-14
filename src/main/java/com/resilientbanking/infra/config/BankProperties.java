package com.resilientbanking.infra.config;

import com.resilientbanking.domain.model.Operation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @param workers       threads que processam pedidos (a "cozinha" do video)
 * @param clientTimeout quanto o cliente espera antes de desistir; resposta depois disso e trabalho perdido
 * @param cost          custo simulado de cada etapa
 * @param accessLog     liga o log de uma linha por pedido
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        int workers,
        Duration clientTimeout,
        Map<Operation, Duration> cost,
        boolean accessLog) {
}
