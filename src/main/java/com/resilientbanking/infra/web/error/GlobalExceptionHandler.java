package com.resilientbanking.infra.web.error;

import com.resilientbanking.domain.exception.AccountNotFoundException;
import com.resilientbanking.domain.exception.IdempotencyConflictException;
import com.resilientbanking.domain.exception.InsufficientFundsException;
import com.resilientbanking.domain.exception.InvalidTransactionException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/** Todas as respostas de erro saem no formato RFC 9457 (application/problem+json) com um codigo estavel. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ProblemDetail> accountNotFound(AccountNotFoundException ex) {
        return problem(ErrorCode.ACCOUNT_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ResponseEntity<ProblemDetail> insufficientFunds(InsufficientFundsException ex) {
        return problem(ErrorCode.INSUFFICIENT_FUNDS, ex.getMessage());
    }

    @ExceptionHandler(InvalidTransactionException.class)
    ResponseEntity<ProblemDetail> invalidTransaction(InvalidTransactionException ex) {
        return problem(ErrorCode.INVALID_TRANSACTION, ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ResponseEntity<ProblemDetail> idempotencyConflict(IdempotencyConflictException ex) {
        return problem(ErrorCode.IDEMPOTENCY_CONFLICT, ex.getMessage());
    }

    /** Duas requisicoes com a mesma chave ao mesmo tempo: a segunda bate na constraint unica. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> dataIntegrity(DataIntegrityViolationException ex) {
        return problem(ErrorCode.CONCURRENT_REQUEST, "Ja existe uma requisicao com esta chave de idempotencia");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> invalidBody(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(ErrorCode.VALIDATION_ERROR, detail);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class,
            MissingRequestHeaderException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> invalidParameter(Exception ex) {
        return problem(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex) {
        return problem(ErrorCode.MALFORMED_REQUEST, "JSON invalido ou com campos de tipo errado");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        // Excecoes do proprio Spring MVC (404, 405, 415...) ja trazem o status certo.
        if (ex instanceof ErrorResponse errorResponse) {
            ProblemDetail body = errorResponse.getBody();
            enrich(body, errorResponse.getStatusCode().is4xxClientError() ? "CLIENT_ERROR" : "SERVER_ERROR");
            return ResponseEntity.status(errorResponse.getStatusCode()).body(body);
        }
        log.error("Erro nao tratado", ex);
        return problem(ErrorCode.INTERNAL_ERROR, "Erro inesperado ao processar a requisicao");
    }

    public static ProblemDetail problemDetail(ErrorCode code, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(code.status(), detail);
        body.setTitle(code.title());
        enrich(body, code.name());
        return body;
    }

    private static ResponseEntity<ProblemDetail> problem(ErrorCode code, String detail) {
        return ResponseEntity.status(code.status()).body(problemDetail(code, detail));
    }

    private static void enrich(ProblemDetail body, String code) {
        body.setProperty("code", code);
        body.setProperty("timestamp", Instant.now());
        body.setProperty("requestId", MDC.get("requestId"));
    }
}
