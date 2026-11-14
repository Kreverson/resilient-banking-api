package com.resilientbanking.domain.model;

import com.resilientbanking.domain.exception.InsufficientFundsException;

import java.math.BigDecimal;

public final class Account {

    public static final long SETTLEMENT_ACCOUNT_ID = 0L;

    private final long id;
    private final String holderName;
    private final AccountKind kind;
    private BigDecimal balance;

    public Account(long id, String holderName, AccountKind kind, BigDecimal balance) {
        this.id = id;
        this.holderName = holderName;
        this.kind = kind;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id);
        }
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public long id() {
        return id;
    }

    public String holderName() {
        return holderName;
    }

    public AccountKind kind() {
        return kind;
    }

    public BigDecimal balance() {
        return balance;
    }
}
