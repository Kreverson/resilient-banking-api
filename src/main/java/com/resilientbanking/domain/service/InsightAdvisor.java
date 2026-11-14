package com.resilientbanking.domain.service;

import com.resilientbanking.domain.model.Account;
import com.resilientbanking.domain.model.EntryDirection;
import com.resilientbanking.domain.model.Insight;
import com.resilientbanking.domain.model.LedgerEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Regras dos insights personalizados do extrato: resumo de movimentacao e ofertas. */
public final class InsightAdvisor {

    private static final BigDecimal INVESTMENT_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal CREDIT_LIMIT_RATIO = new BigDecimal("0.30");

    public List<Insight> advise(Account account, List<LedgerEntry> recentEntries) {
        BigDecimal outflow = sum(recentEntries, EntryDirection.DEBIT);
        BigDecimal inflow = sum(recentEntries, EntryDirection.CREDIT);

        List<Insight> insights = new ArrayList<>();
        insights.add(new Insight("CASH_FLOW",
                "Nas ultimas " + recentEntries.size() + " movimentacoes entraram R$ " + inflow
                        + " e sairam R$ " + outflow));
        if (outflow.compareTo(inflow) > 0) {
            insights.add(new Insight("SPENDING_ALERT", "Seus gastos recentes superaram suas entradas"));
        }
        if (account.balance().compareTo(INVESTMENT_THRESHOLD) >= 0) {
            insights.add(new Insight("INVESTMENT_OFFER", "Voce tem saldo parado: conheca o CDB de liquidez diaria"));
        }
        BigDecimal preApproved = account.balance().multiply(CREDIT_LIMIT_RATIO).setScale(2, RoundingMode.DOWN);
        insights.add(new Insight("CREDIT_OFFER", "Credito pre-aprovado de R$ " + preApproved));
        return insights;
    }

    private static BigDecimal sum(List<LedgerEntry> entries, EntryDirection direction) {
        return entries.stream()
                .filter(entry -> entry.direction() == direction)
                .map(LedgerEntry::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
