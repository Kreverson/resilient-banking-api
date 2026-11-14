package com.resilientbanking.infra.web.dto;

import java.math.BigDecimal;

public record BatchResponse(String partnerId, int records, BigDecimal total, String node) {
}
