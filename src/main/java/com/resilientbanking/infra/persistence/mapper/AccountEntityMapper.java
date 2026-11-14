package com.resilientbanking.infra.persistence.mapper;

import com.resilientbanking.domain.model.Account;
import com.resilientbanking.infra.persistence.entity.AccountEntity;

public final class AccountEntityMapper {

    private AccountEntityMapper() {
    }

    public static Account toDomain(AccountEntity entity) {
        return new Account(entity.getId(), entity.getHolderName(), entity.getKind(), entity.getBalance());
    }
}
