package com.resilientbanking.infra.execution;

import java.util.Locale;
import java.util.Optional;

/**
 * Criticidade do pedido, inspirada nos niveis do Google (CRITICAL_PLUS, CRITICAL, SHEDDABLE_PLUS, SHEDDABLE).
 * O app envia no header X-Request-Priority; sem header, a prioridade sai da rota.
 */
public enum RequestPriority {
    CRITICAL,
    IMPORTANT,
    SHEDDABLE;

    public static Optional<RequestPriority> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String tag() {
        return name().toLowerCase(Locale.ROOT);
    }
}
