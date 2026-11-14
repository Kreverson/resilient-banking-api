package com.resilientbanking.infra.observability;

import com.resilientbanking.infra.execution.RequestPriority;

/** Tempos de um pedido: chegada na porta, inicio na worker e fim. */
public final class RequestTrace {

    private final String requestId;
    private final String method;
    private final String path;
    private final RequestPriority priority;
    private final long arrivalNanos;
    private volatile long startNanos;
    private volatile long endNanos;
    private volatile int status;

    public RequestTrace(String requestId, String method, String path, RequestPriority priority, long arrivalNanos) {
        this.requestId = requestId;
        this.method = method;
        this.path = path;
        this.priority = priority;
        this.arrivalNanos = arrivalNanos;
    }

    public void started(long nanos) {
        this.startNanos = nanos;
    }

    public void finished(long nanos, int status) {
        if (startNanos == 0) {
            startNanos = nanos;
        }
        this.endNanos = nanos;
        this.status = status;
    }

    public long queueNanos() {
        return startNanos - arrivalNanos;
    }

    public long serviceNanos() {
        return endNanos - startNanos;
    }

    public long totalNanos() {
        return endNanos - arrivalNanos;
    }

    public String requestId() {
        return requestId;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public RequestPriority priority() {
        return priority;
    }

    public long arrivalNanos() {
        return arrivalNanos;
    }

    public int status() {
        return status;
    }
}
