package com.resilientbanking.domain.model;

import com.resilientbanking.domain.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.resilientbanking.domain.model.Account.SETTLEMENT_ACCOUNT_ID;

public record Transaction(
        UUID id,
        String idempotencyKey,
        TransactionType type,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal amount,
        Instant createdAt) {

    public static Transaction create(String idempotencyKey, TransactionType type,
                                     Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new InvalidTransactionException("O valor deve ser positivo e ter no maximo 2 casas decimais");
        }
        switch (type) {
            case DEPOSIT -> require(targetAccountId != null && sourceAccountId == null,
                    "Deposito exige apenas a conta de destino");
            case WITHDRAWAL -> require(sourceAccountId != null && targetAccountId == null,
                    "Saque exige apenas a conta de origem");
            case TRANSFER -> require(sourceAccountId != null && targetAccountId != null
                            && !sourceAccountId.equals(targetAccountId),
                    "Transferencia exige contas de origem e destino diferentes");
        }
        return new Transaction(UUID.randomUUID(), idempotencyKey, type, sourceAccountId, targetAccountId,
                amount.setScale(2), Instant.now());
    }

    /** Contas de clientes cujo saldo sera alterado (a conta de liquidacao fica de fora). */
    public Set<Long> customerAccountIds() {
        return switch (type) {
            case DEPOSIT -> Set.of(targetAccountId);
            case WITHDRAWAL -> Set.of(sourceAccountId);
            case TRANSFER -> Set.of(sourceAccountId, targetAccountId);
        };
    }

    /** Aplica a transacao nas contas e devolve os dois lados do lancamento. */
    public List<LedgerEntry> post(Map<Long, Account> accounts) {
        long debitAccount = sourceAccountId != null ? sourceAccountId : SETTLEMENT_ACCOUNT_ID;
        long creditAccount = targetAccountId != null ? targetAccountId : SETTLEMENT_ACCOUNT_ID;
        return List.of(
                apply(accounts.get(debitAccount), debitAccount, EntryDirection.DEBIT),
                apply(accounts.get(creditAccount), creditAccount, EntryDirection.CREDIT));
    }

    /** A mesma chave de idempotencia so pode ser reaproveitada com o mesmo conteudo. */
    public boolean samePayload(TransactionType type, Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        return this.type == type
                && Objects.equals(this.sourceAccountId, sourceAccountId)
                && Objects.equals(this.targetAccountId, targetAccountId)
                && this.amount.compareTo(amount) == 0;
    }

    private LedgerEntry apply(Account account, long accountId, EntryDirection direction) {
        if (account == null) {
            return new LedgerEntry(id, accountId, direction, amount, null, createdAt);
        }
        if (direction == EntryDirection.DEBIT) {
            account.debit(amount);
        } else {
            account.credit(amount);
        }
        return new LedgerEntry(id, accountId, direction, amount, account.balance(), createdAt);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new InvalidTransactionException(message);
        }
    }
}
