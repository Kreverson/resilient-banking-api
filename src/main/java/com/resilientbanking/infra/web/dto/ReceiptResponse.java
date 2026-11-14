package com.resilientbanking.infra.web.dto;

import com.resilientbanking.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReceiptResponse(
        UUID transactionId,
        TransactionType type,
        BigDecimal amount,
        Instant createdAt,
        String authenticationCode) {
}
