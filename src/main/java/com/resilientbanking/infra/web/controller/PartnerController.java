package com.resilientbanking.infra.web.controller;

import com.resilientbanking.application.usecase.ProcessBatchUseCase;
import com.resilientbanking.infra.web.dto.BatchResponse;
import com.resilientbanking.infra.web.mapper.WebMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/partners/{partnerId}")
public class PartnerController {

    private final ProcessBatchUseCase processBatch;
    private final String node;

    public PartnerController(ProcessBatchUseCase processBatch, @Value("${NODE_ID:local}") String node) {
        this.processBatch = processBatch;
        this.node = node;
    }

    @PostMapping(path = "/batches", consumes = MediaType.TEXT_PLAIN_VALUE)
    public BatchResponse batch(@PathVariable String partnerId, @RequestBody String content) {
        return WebMapper.toResponse(processBatch.execute(partnerId, content), node);
    }
}
