package com.resilientbanking.infra.web.mapper;

import com.resilientbanking.application.command.TransactionCommand;
import com.resilientbanking.application.result.TransactionResult;
import com.resilientbanking.domain.model.PaymentBatch;
import com.resilientbanking.domain.model.Receipt;
import com.resilientbanking.domain.model.Statement;
import com.resilientbanking.domain.model.Transaction;
import com.resilientbanking.infra.web.dto.BatchResponse;
import com.resilientbanking.infra.web.dto.ReceiptResponse;
import com.resilientbanking.infra.web.dto.StatementResponse;
import com.resilientbanking.infra.web.dto.TransactionRequest;
import com.resilientbanking.infra.web.dto.TransactionResponse;

import java.util.List;

public final class WebMapper {

    private WebMapper() {
    }

    public static TransactionCommand toCommand(String idempotencyKey, TransactionRequest request) {
        return new TransactionCommand(idempotencyKey, request.type(), request.sourceAccountId(),
                request.targetAccountId(), request.amount());
    }

    public static TransactionResponse toResponse(TransactionResult result) {
        Transaction transaction = result.transaction();
        return new TransactionResponse(transaction.id(), transaction.type(), transaction.sourceAccountId(),
                transaction.targetAccountId(), transaction.amount(), transaction.createdAt(), result.replayed());
    }

    public static StatementResponse toResponse(Statement statement) {
        var entries = statement.entries().stream()
                .map(entry -> new StatementResponse.Entry(entry.transactionId(), entry.direction(), entry.amount(),
                        entry.balanceAfter(), entry.createdAt()))
                .toList();
        List<StatementResponse.Insight> insights = statement.insights() == null ? null
                : statement.insights().stream()
                .map(insight -> new StatementResponse.Insight(insight.type(), insight.message()))
                .toList();
        return new StatementResponse(statement.account().id(), statement.account().holderName(),
                statement.account().balance(), entries, insights, statement.degraded());
    }

    public static BatchResponse toResponse(PaymentBatch batch, String node) {
        return new BatchResponse(batch.partnerId(), batch.lines().size(), batch.total(), node);
    }

    public static ReceiptResponse toResponse(Receipt receipt) {
        Transaction transaction = receipt.transaction();
        return new ReceiptResponse(transaction.id(), transaction.type(), transaction.amount(),
                transaction.createdAt(), receipt.authenticationCode());
    }
}
