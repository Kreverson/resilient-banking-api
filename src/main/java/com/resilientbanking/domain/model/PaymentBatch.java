package com.resilientbanking.domain.model;

import com.resilientbanking.domain.exception.InvalidBatchException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lote de pagamentos enviado por um parceiro (um CNAB simplificado), uma linha por registro:
 * <pre>
 * H|&lt;parceiro&gt;|&lt;quantidade de registros&gt;
 * D|&lt;conta&gt;|&lt;valor&gt;
 * T|&lt;quantidade&gt;|&lt;total&gt;
 * </pre>
 */
public record PaymentBatch(String partnerId, List<PaymentLine> lines, BigDecimal total) {

    public record PaymentLine(long accountId, BigDecimal amount) {
    }

    public static PaymentBatch parse(String partnerId, String content) {
        String[] rows = content.strip().split("\\R");
        String[] header = fields(rows[0], "H", 3);
        int declared = number(header[2]);

        // BUG PROPOSITAL (o "pedido envenenado" do video): o parser confia na quantidade declarada no header para
        // pre-alocar os registros, antes de conferir com o trailer. Um header com quantidade absurda estoura a
        // memoria e derruba a instancia inteira (-XX:+ExitOnOutOfMemoryError), junto com todo mundo que ela atendia.
        PaymentLine[] buffer = new PaymentLine[declared];

        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i < rows.length - 1; i++) {
            String[] detail = fields(rows[i], "D", 3);
            PaymentLine line = new PaymentLine(number(detail[1]), new BigDecimal(detail[2]));
            if (count >= buffer.length) {
                throw new InvalidBatchException("Lote com mais registros do que o declarado no header");
            }
            buffer[count++] = line;
            total = total.add(line.amount());
        }
        String[] trailer = fields(rows[rows.length - 1], "T", 3);
        if (number(trailer[1]) != count || new BigDecimal(trailer[2]).compareTo(total) != 0) {
            throw new InvalidBatchException("Trailer nao confere com os registros do lote");
        }
        return new PaymentBatch(partnerId, new ArrayList<>(Arrays.asList(buffer).subList(0, count)), total);
    }

    private static String[] fields(String row, String type, int expected) {
        String[] parts = row.split("\\|");
        if (parts.length != expected || !parts[0].equals(type)) {
            throw new InvalidBatchException("Registro invalido, esperado tipo " + type + ": " + row);
        }
        return parts;
    }

    private static int number(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new InvalidBatchException("Numero invalido no lote: " + value);
        }
    }
}
