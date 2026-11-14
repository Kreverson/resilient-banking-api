package com.resilientbanking.domain.model;

import java.util.List;

/** insights nulo significa extrato degradado: saiu sem a parte personalizada. */
public record Statement(Account account, List<LedgerEntry> entries, List<Insight> insights) {

    public boolean degraded() {
        return insights == null;
    }
}
