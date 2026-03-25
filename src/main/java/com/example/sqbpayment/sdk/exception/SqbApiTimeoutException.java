package com.example.sqbpayment.sdk.exception;

/**
 * 请求超时异常
 */
public class SqbApiTimeoutException extends SqbApiConnectionException {

    private final Integer connectTimeoutMs;
    private final Integer readTimeoutMs;

    public SqbApiTimeoutException(String message, Throwable cause, Integer connectTimeoutMs, Integer readTimeoutMs) {
        super(message, cause);
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }
}
