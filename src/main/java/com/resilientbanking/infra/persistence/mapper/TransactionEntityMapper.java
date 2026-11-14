package com.resilientbanking.infra.persistence.mapper;

import com.resilientbanking.domain.model.LedgerEntry;
import com.resilientbanking.domain.model.Transaction;
import com.resilientbanking.infra.persistence.entity.LedgerEntryEntity;
import com.resilientbanking.infra.persistence.entity.TransactionEntity;

public final class TransactionEntityMapper {

    private TransactionEntityMapper() {
    }

    public static TransactionEntity toEntity(Transaction transaction) {
        return new TransactionEntity(transaction.id(), transaction.idempotencyKey(), transaction.type(),
                transaction.sourceAccountId(), transaction.targetAccountId(), transaction.amount(),
                transaction.createdAt());
    }

    public static Transaction toDomain(TransactionEntity entity) {
        return new Transaction(entity.getId(), entity.getIdempotencyKey(), entity.getType(),
                entity.getSourceAccountId(), entity.getTargetAccountId(), entity.getAmount(),
                entity.getCreatedAt());
    }

    public static LedgerEntryEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(entry.transactionId(), entry.accountId(), entry.direction(), entry.amount(),
                entry.balanceAfter(), entry.createdAt());
    }

    public static LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(entity.getTransactionId(), entity.getAccountId(), entity.getDirection(),
                entity.getAmount(), entity.getBalanceAfter(), entity.getCreatedAt());
    }
}
