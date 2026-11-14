package com.resilientbanking.infra.web.controller;

import com.resilientbanking.application.usecase.GetStatementUseCase;
import com.resilientbanking.application.usecase.PrefetchReceiptsUseCase;
import com.resilientbanking.infra.web.dto.ReceiptResponse;
import com.resilientbanking.infra.web.dto.StatementResponse;
import com.resilientbanking.infra.web.mapper.WebMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/accounts/{accountId}")
public class AccountController {

    private final GetStatementUseCase getStatement;
    private final PrefetchReceiptsUseCase prefetchReceipts;

    public AccountController(GetStatementUseCase getStatement, PrefetchReceiptsUseCase prefetchReceipts) {
        this.getStatement = getStatement;
        this.prefetchReceipts = prefetchReceipts;
    }

    @GetMapping("/statement")
    public StatementResponse statement(@PathVariable long accountId) {
        return WebMapper.toResponse(getStatement.execute(accountId));
    }

    @GetMapping("/receipts")
    public List<ReceiptResponse> receipts(@PathVariable long accountId) {
        return prefetchReceipts.execute(accountId).stream().map(WebMapper::toResponse).toList();
    }
}
