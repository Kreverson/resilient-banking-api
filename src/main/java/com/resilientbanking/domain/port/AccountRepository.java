package com.resilientbanking.domain.port;

import com.resilientbanking.domain.model.Account;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(long id);

    /** Busca e trava as contas (SELECT ... FOR UPDATE) sempre na mesma ordem para evitar deadlock. */
    List<Account> lockAll(Collection<Long> ids);

    void updateBalances(Collection<Account> accounts);
}
