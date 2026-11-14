package com.resilientbanking.infra.persistence.entity;

import com.resilientbanking.domain.model.AccountKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private Long id;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountKind kind;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected AccountEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getHolderName() {
        return holderName;
    }

    public AccountKind getKind() {
        return kind;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
