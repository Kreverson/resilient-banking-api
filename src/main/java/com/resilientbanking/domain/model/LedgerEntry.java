package com.resilientbanking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Um lado do lancamento em partidas dobradas. Toda transacao gera um debito e um credito de mesmo valor.
 * A conta de liquidacao nao tem saldo controlado, por isso balanceAfter fica nulo nela.
 */
public record LedgerEntry(
        UUID transactionId,
        long accountId,
        EntryDirection direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Instant createdAt) {
}
