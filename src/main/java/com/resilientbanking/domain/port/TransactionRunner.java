package com.resilientbanking.domain.port;

import java.util.function.Supplier;

/** Delimita uma transacao de banco sem acoplar o dominio ao Spring. */
public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);
}
