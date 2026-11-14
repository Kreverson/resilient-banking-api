package com.resilientbanking.application.usecase;

import com.resilientbanking.application.command.TransactionCommand;
import com.resilientbanking.application.result.TransactionResult;
import com.resilientbanking.domain.exception.AccountNotFoundException;
import com.resilientbanking.domain.exception.IdempotencyConflictException;
import com.resilientbanking.domain.model.Account;
import com.resilientbanking.domain.model.LedgerEntry;
import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.domain.model.Transaction;
import com.resilientbanking.domain.port.AccountRepository;
import com.resilientbanking.domain.port.ProcessingCost;
import com.resilientbanking.domain.port.TransactionRepository;
import com.resilientbanking.domain.port.TransactionRunner;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Deposito, saque ou transferencia. E o pedido critico da API (o "pedido de comida" do video). */
public class ExecuteTransactionUseCase {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final TransactionRunner transactionRunner;
    private final ProcessingCost processingCost;

    public ExecuteTransactionUseCase(AccountRepository accounts, TransactionRepository transactions,
                                     TransactionRunner transactionRunner, ProcessingCost processingCost) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.transactionRunner = transactionRunner;
        this.processingCost = processingCost;
    }

    public TransactionResult execute(TransactionCommand command) {
        var existing = transactions.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return replay(existing.get(), command);
        }

        Transaction transaction = Transaction.create(command.idempotencyKey(), command.type(),
                command.sourceAccountId(), command.targetAccountId(), command.amount());

        // Antifraude roda antes de abrir a transacao para nao segurar lock de conta durante a analise.
        processingCost.spend(Operation.FRAUD_CHECK);

        return transactionRunner.inTransaction(() -> {
            Map<Long, Account> locked = accounts.lockAll(transaction.customerAccountIds()).stream()
                    .collect(Collectors.toMap(Account::id, Function.identity()));
            transaction.customerAccountIds().stream()
                    .filter(id -> !locked.containsKey(id))
                    .findFirst()
                    .ifPresent(id -> {
                        throw new AccountNotFoundException(id);
                    });

            List<LedgerEntry> entries = transaction.post(locked);
            accounts.updateBalances(locked.values());
            transactions.save(transaction, entries);
            return new TransactionResult(transaction, false);
        });
    }

    private static TransactionResult replay(Transaction existing, TransactionCommand command) {
        if (!existing.samePayload(command.type(), command.sourceAccountId(), command.targetAccountId(),
                command.amount())) {
            throw new IdempotencyConflictException(command.idempotencyKey());
        }
        return new TransactionResult(existing, true);
    }
}
