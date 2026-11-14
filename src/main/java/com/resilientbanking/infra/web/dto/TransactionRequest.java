package com.resilientbanking.infra.web.dto;

import com.resilientbanking.domain.model.TransactionType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull TransactionType type,
        @Positive Long sourceAccountId,
        @Positive Long targetAccountId,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
