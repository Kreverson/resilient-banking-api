package com.resilientbanking.infra.config;

import com.resilientbanking.application.usecase.ExecuteTransactionUseCase;
import com.resilientbanking.application.usecase.GetStatementUseCase;
import com.resilientbanking.application.usecase.PrefetchReceiptsUseCase;
import com.resilientbanking.application.usecase.ProcessBatchUseCase;
import com.resilientbanking.domain.port.AccountRepository;
import com.resilientbanking.domain.port.ProcessingCost;
import com.resilientbanking.domain.port.TransactionRepository;
import com.resilientbanking.domain.port.TransactionRunner;
import com.resilientbanking.domain.service.InsightAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta os casos de uso; application e domain nao conhecem o Spring. */
@Configuration
public class UseCaseConfig {

    @Bean
    ExecuteTransactionUseCase executeTransactionUseCase(AccountRepository accounts, TransactionRepository transactions,
                                                        TransactionRunner runner, ProcessingCost cost) {
        return new ExecuteTransactionUseCase(accounts, transactions, runner, cost);
    }

    @Bean
    GetStatementUseCase getStatementUseCase(AccountRepository accounts, TransactionRepository transactions,
                                            ProcessingCost cost) {
        return new GetStatementUseCase(accounts, transactions, cost, new InsightAdvisor());
    }

    @Bean
    ProcessBatchUseCase processBatchUseCase(ProcessingCost cost) {
        return new ProcessBatchUseCase(cost);
    }

    @Bean
    PrefetchReceiptsUseCase prefetchReceiptsUseCase(AccountRepository accounts, TransactionRepository transactions,
                                                    ProcessingCost cost) {
        return new PrefetchReceiptsUseCase(accounts, transactions, cost);
    }
}
