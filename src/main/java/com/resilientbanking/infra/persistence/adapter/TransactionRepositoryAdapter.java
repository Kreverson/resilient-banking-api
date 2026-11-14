package com.resilientbanking.infra.persistence.adapter;

import com.resilientbanking.domain.model.LedgerEntry;
import com.resilientbanking.domain.model.Transaction;
import com.resilientbanking.domain.port.TransactionRepository;
import com.resilientbanking.infra.persistence.mapper.TransactionEntityMapper;
import com.resilientbanking.infra.persistence.repository.LedgerEntryJpaRepository;
import com.resilientbanking.infra.persistence.repository.TransactionJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository transactions;
    private final LedgerEntryJpaRepository entries;
    private final EntityManager entityManager;

    public TransactionRepositoryAdapter(TransactionJpaRepository transactions, LedgerEntryJpaRepository entries,
                                        EntityManager entityManager) {
        this.transactions = transactions;
        this.entries = entries;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return transactions.findByIdempotencyKey(idempotencyKey).map(TransactionEntityMapper::toDomain);
    }

    @Override
    public void save(Transaction transaction, List<LedgerEntry> ledgerEntries) {
        // persist em vez de save: o id ja vem do dominio e o save faria um SELECT antes do INSERT.
        entityManager.persist(TransactionEntityMapper.toEntity(transaction));
        ledgerEntries.forEach(entry -> entityManager.persist(TransactionEntityMapper.toEntity(entry)));
    }

    @Override
    public List<Transaction> findRecentByAccount(long accountId, int limit) {
        return transactions.findRecentByAccount(accountId, Limit.of(limit)).stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<LedgerEntry> findRecentEntriesByAccount(long accountId, int limit) {
        return entries.findByAccountIdOrderByIdDesc(accountId, Limit.of(limit)).stream()
                .map(TransactionEntityMapper::toDomain)
                .toList();
    }
}
