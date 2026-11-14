package com.resilientbanking.infra.web.dto;

import com.resilientbanking.domain.model.EntryDirection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** degraded indica que a resposta saiu sem os insights personalizados. */
public record StatementResponse(
        long accountId,
        String holderName,
        BigDecimal balance,
        List<Entry> entries,
        List<Insight> insights,
        boolean degraded) {

    public record Entry(UUID transactionId, EntryDirection direction, BigDecimal amount, BigDecimal balanceAfter,
                        Instant createdAt) {
    }

    public record Insight(String type, String message) {
    }
}
