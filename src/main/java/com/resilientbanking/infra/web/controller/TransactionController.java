package com.resilientbanking.infra.web.controller;

import com.resilientbanking.application.usecase.ExecuteTransactionUseCase;
import com.resilientbanking.infra.web.dto.TransactionRequest;
import com.resilientbanking.infra.web.dto.TransactionResponse;
import com.resilientbanking.infra.web.mapper.WebMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final ExecuteTransactionUseCase executeTransaction;

    public TransactionController(ExecuteTransactionUseCase executeTransaction) {
        this.executeTransaction = executeTransaction;
    }

    /** 201 quando lancou agora, 200 quando a chave de idempotencia ja tinha sido processada. */
    @PostMapping
    public ResponseEntity<TransactionResponse> execute(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @RequestBody @Valid TransactionRequest request) {
        var result = executeTransaction.execute(WebMapper.toCommand(idempotencyKey, request));
        var status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(WebMapper.toResponse(result));
    }
}
