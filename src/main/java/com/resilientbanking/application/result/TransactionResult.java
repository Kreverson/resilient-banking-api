package com.resilientbanking.application.result;

import com.resilientbanking.domain.model.Transaction;

/** replayed indica que a chave de idempotencia ja existia e nada foi lancado de novo. */
public record TransactionResult(Transaction transaction, boolean replayed) {
}
