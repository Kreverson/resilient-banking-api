package com.resilientbanking.infra.config;

import com.resilientbanking.domain.model.Operation;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @param workers       threads que processam pedidos (a "cozinha" do video)
 * @param clientTimeout quanto o cliente espera antes de desistir; resposta depois disso e trabalho perdido
 * @param lifo          LIFO adaptativo
 * @param codel         descarte por tempo de fila (controlled delay)
 * @param cost          custo simulado de cada etapa
 * @param accessLog     liga o log de uma linha por pedido
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        int workers,
        Duration clientTimeout,
        Lifo lifo,
        Codel codel,
        Map<Operation, Duration> cost,
        boolean accessLog) {

    /** @param threshold tamanho da fila a partir do qual a worker passa a pegar o pedido mais novo */
    public record Lifo(boolean enabled, int threshold) {
    }

    /**
     * @param interval se a fila nao esvaziou nesse intervalo, o sistema esta sobrecarregado; e tambem o limite de
     *                 espera em situacao normal
     * @param target   limite de espera quando sobrecarregado
     */
    public record Codel(boolean enabled, Duration interval, Duration target) {
    }
}
