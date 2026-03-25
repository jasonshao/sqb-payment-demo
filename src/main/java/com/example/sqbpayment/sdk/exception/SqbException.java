package com.example.sqbpayment.sdk.exception;

/**
 * 收钱吧 SDK 基础异常
 */
public class SqbException extends RuntimeException {

    private final String requestId;
    private final Integer httpStatus;
    private final boolean retryable;
    private final String userMessage;

    public SqbException(String message, String requestId, Integer httpStatus, boolean retryable, String userMessage, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.userMessage = userMessage;
    }

    public SqbException(String message, Throwable cause) {
        this(message, null, null, false, message, cause);
    }

    public SqbException(String message) {
        this(message, null, null, false, message, null);
    }

    public String getRequestId() {
        return requestId;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
