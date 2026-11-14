package com.resilientbanking.infra.config;

import com.resilientbanking.domain.model.Operation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @param workers       threads que processam pedidos (a "cozinha" do video)
 * @param queueCapacity lugares na fila; quem chega com a fila cheia recebe 503
 * @param retryAfter    valor do header Retry-After da recusa
 * @param clientTimeout quanto o cliente espera antes de desistir; resposta depois disso e trabalho perdido
 * @param cost          custo simulado de cada etapa
 * @param accessLog     liga o log de uma linha por pedido
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        int workers,
        int queueCapacity,
        Duration retryAfter,
        Duration clientTimeout,
        Map<Operation, Duration> cost,
        boolean accessLog) {
}
