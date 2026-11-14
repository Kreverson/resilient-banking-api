package com.resilientbanking.domain.exception;

public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(long accountId) {
        super("Conta " + accountId + " nao encontrada");
    }
}
