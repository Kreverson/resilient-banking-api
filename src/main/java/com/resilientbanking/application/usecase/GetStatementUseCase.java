package com.resilientbanking.application.usecase;

import com.resilientbanking.domain.exception.AccountNotFoundException;
import com.resilientbanking.domain.model.Account;
import com.resilientbanking.domain.model.Insight;
import com.resilientbanking.domain.model.LedgerEntry;
import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.domain.model.Statement;
import com.resilientbanking.domain.port.AccountRepository;
import com.resilientbanking.domain.port.ProcessingCost;
import com.resilientbanking.domain.port.TransactionRepository;
import com.resilientbanking.domain.service.InsightAdvisor;

import java.util.List;

/** Saldo e extrato com insights personalizados (o "cardapio" do video). */
public class GetStatementUseCase {

    private static final int STATEMENT_SIZE = 20;

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final ProcessingCost processingCost;
    private final InsightAdvisor insightAdvisor;

    public GetStatementUseCase(AccountRepository accounts, TransactionRepository transactions,
                               ProcessingCost processingCost, InsightAdvisor insightAdvisor) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.processingCost = processingCost;
        this.insightAdvisor = insightAdvisor;
    }

    public Statement execute(long accountId) {
        Account account = accounts.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        List<LedgerEntry> entries = transactions.findRecentEntriesByAccount(accountId, STATEMENT_SIZE);
        processingCost.spend(Operation.STATEMENT);

        // Parte cara e opcional da resposta: e ela que a degradacao graciosa desliga sob carga.
        processingCost.spend(Operation.INSIGHTS);
        List<Insight> insights = insightAdvisor.advise(account, entries);

        return new Statement(account, entries, insights);
    }
}
