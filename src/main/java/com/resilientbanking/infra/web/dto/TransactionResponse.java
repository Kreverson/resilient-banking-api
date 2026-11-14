package com.resilientbanking.infra.web.dto;

import com.resilientbanking.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal amount,
        Instant createdAt,
        boolean replayed) {
}
