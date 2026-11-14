package com.resilientbanking.domain.exception;

public class InsufficientFundsException extends DomainException {

    public InsufficientFundsException(long accountId) {
        super("Saldo insuficiente na conta " + accountId);
    }
}
