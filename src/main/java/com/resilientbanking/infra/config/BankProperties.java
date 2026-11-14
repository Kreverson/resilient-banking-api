package com.resilientbanking.infra.config;

import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.infra.execution.RequestPriority;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @param workers       threads que processam pedidos (a "cozinha" do video)
 * @param queueLimits   tamanho de fila ate o qual cada prioridade ainda entra
 * @param retryAfter    valor do header Retry-After da recusa
 * @param clientTimeout quanto o cliente espera antes de desistir; resposta depois disso e trabalho perdido
 * @param brownout      controlador da degradacao graciosa
 * @param cost          custo simulado de cada etapa
 * @param accessLog     liga o log de uma linha por pedido
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        int workers,
        Map<RequestPriority, Integer> queueLimits,
        Duration retryAfter,
        Duration clientTimeout,
        Brownout brownout,
        Map<Operation, Duration> cost,
        boolean accessLog) {

    /**
     * @param interval  de quanto em quanto tempo o controlador olha a fila
     * @param queueHigh acima disso o dimmer desce
     * @param queueLow  abaixo disso o dimmer sobe
     * @param stepDown  quanto o dimmer desce por intervalo
     * @param stepUp    quanto o dimmer sobe por intervalo
     */
    public record Brownout(Duration interval, int queueHigh, int queueLow, double stepDown, double stepUp) {
    }
}
