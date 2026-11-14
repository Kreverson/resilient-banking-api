package com.resilientbanking.domain.model;

import java.util.List;

public record Statement(Account account, List<LedgerEntry> entries, List<Insight> insights) {
}
