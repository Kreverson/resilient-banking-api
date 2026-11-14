package com.resilientbanking.infra.persistence.adapter;

import com.resilientbanking.domain.model.Account;
import com.resilientbanking.domain.port.AccountRepository;
import com.resilientbanking.infra.persistence.mapper.AccountEntityMapper;
import com.resilientbanking.infra.persistence.repository.AccountJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;

    public AccountRepositoryAdapter(AccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Account> findById(long id) {
        return jpa.findById(id).map(AccountEntityMapper::toDomain);
    }

    @Override
    public List<Account> lockAll(Collection<Long> ids) {
        return jpa.findAllForUpdate(ids).stream().map(AccountEntityMapper::toDomain).toList();
    }

    @Override
    public void updateBalances(Collection<Account> accounts) {
        // As entidades ja estao no contexto de persistencia (travadas em lockAll); o flush grava o novo saldo.
        accounts.forEach(account -> jpa.getReferenceById(account.id()).setBalance(account.balance()));
    }
}
