package com.resilientbanking.infra.execution;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PriorityClassifier {

    public static final String PRIORITY_HEADER = "X-Request-Priority";

    public RequestPriority classify(HttpServletRequest request) {
        return RequestPriority.parse(request.getHeader(PRIORITY_HEADER))
                .orElseGet(() -> byRoute(request));
    }

    private static RequestPriority byRoute(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/v1/transactions")) {
            return RequestPriority.CRITICAL;
        }
        if (path.endsWith("/receipts")) {
            return RequestPriority.SHEDDABLE;
        }
        return RequestPriority.IMPORTANT;
    }
}
