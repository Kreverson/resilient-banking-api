package com.resilientbanking.application.usecase;

import com.resilientbanking.domain.model.Operation;
import com.resilientbanking.domain.model.PaymentBatch;
import com.resilientbanking.domain.port.ProcessingCost;

/** Recebe o lote de pagamentos de um parceiro (BaaS) e devolve o resumo validado. */
public class ProcessBatchUseCase {

    private final ProcessingCost processingCost;

    public ProcessBatchUseCase(ProcessingCost processingCost) {
        this.processingCost = processingCost;
    }

    public PaymentBatch execute(String partnerId, String content) {
        PaymentBatch batch = PaymentBatch.parse(partnerId, content);
        processingCost.spend(Operation.BATCH);
        return batch;
    }
}
