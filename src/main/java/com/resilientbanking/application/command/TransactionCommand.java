package com.resilientbanking.application.command;

import com.resilientbanking.domain.model.TransactionType;

import java.math.BigDecimal;

public record TransactionCommand(
        String idempotencyKey,
        TransactionType type,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal amount) {
}
