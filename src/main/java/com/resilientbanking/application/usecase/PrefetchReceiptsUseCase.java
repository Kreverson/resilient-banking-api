package com.resilientbanking.application.usecase;

import com.resilientbanking.domain.exception.AccountNotFoundException;
import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.domain.model.Receipt;
import com.resilientbanking.domain.model.Transaction;
import com.resilientbanking.domain.port.AccountRepository;
import com.resilientbanking.domain.port.ProcessingCost;
import com.resilientbanking.domain.port.TransactionRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Comprovantes que o app pre-carrega enquanto a pessoa navega (o "segundo plano" do video). */
public class PrefetchReceiptsUseCase {

    private static final int RECEIPTS_SIZE = 10;

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final ProcessingCost processingCost;

    public PrefetchReceiptsUseCase(AccountRepository accounts, TransactionRepository transactions,
                                   ProcessingCost processingCost) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.processingCost = processingCost;
    }

    public List<Receipt> execute(long accountId) {
        if (accounts.findById(accountId).isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        List<Transaction> recent = transactions.findRecentByAccount(accountId, RECEIPTS_SIZE);
        processingCost.spend(Operation.RECEIPTS);
        return recent.stream()
                .map(transaction -> new Receipt(transaction, authenticationCode(transaction)))
                .toList();
    }

    private static String authenticationCode(Transaction transaction) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(transaction.id().toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
