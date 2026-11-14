package com.resilientbanking.infra.web.error;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escreve um erro no mesmo formato do {@link GlobalExceptionHandler}, mas direto na resposta.
 * E usado pela porta (filtro), que recusa pedidos antes do Spring MVC e sem ler o corpo: recusar precisa ser barato.
 */
public final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    public static void write(HttpServletResponse response, ErrorCode code, String detail, String instance,
                             String requestId) throws IOException {
        String json = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","code":"%s",\
                "timestamp":"%s","requestId":"%s"}"""
                .formatted(escape(code.title()), code.status().value(), escape(detail), escape(instance), code.name(),
                        Instant.now(), escape(requestId));
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
