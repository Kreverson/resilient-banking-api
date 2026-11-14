CREATE TABLE accounts (
    id          BIGINT PRIMARY KEY,
    holder_name VARCHAR(120)   NOT NULL,
    kind        VARCHAR(20)    NOT NULL,
    balance     NUMERIC(19, 2) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_accounts_kind CHECK (kind IN ('CUSTOMER', 'SETTLEMENT')),
    CONSTRAINT ck_accounts_balance CHECK (kind = 'SETTLEMENT' OR balance >= 0)
);

CREATE TABLE transactions (
    id                UUID PRIMARY KEY,
    idempotency_key   VARCHAR(100)   NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    source_account_id BIGINT REFERENCES accounts (id),
    target_account_id BIGINT REFERENCES accounts (id),
    amount            NUMERIC(19, 2) NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_transactions_type CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT ck_transactions_amount CHECK (amount > 0)
);

CREATE TABLE ledger_entries (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id UUID           NOT NULL REFERENCES transactions (id),
    account_id     BIGINT         NOT NULL REFERENCES accounts (id),
    direction      VARCHAR(6)     NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    balance_after  NUMERIC(19, 2),
    created_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_ledger_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX ix_ledger_entries_account ON ledger_entries (account_id, id DESC);
CREATE INDEX ix_transactions_source ON transactions (source_account_id, created_at DESC);
CREATE INDEX ix_transactions_target ON transactions (target_account_id, created_at DESC);
