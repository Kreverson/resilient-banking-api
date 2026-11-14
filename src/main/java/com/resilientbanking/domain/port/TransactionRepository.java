package com.resilientbanking.domain.port;

import com.resilientbanking.domain.model.LedgerEntry;
import com.resilientbanking.domain.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    void save(Transaction transaction, List<LedgerEntry> entries);

    List<Transaction> findRecentByAccount(long accountId, int limit);

    List<LedgerEntry> findRecentEntriesByAccount(long accountId, int limit);
}
