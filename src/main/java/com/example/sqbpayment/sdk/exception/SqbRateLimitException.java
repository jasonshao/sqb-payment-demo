package com.example.sqbpayment.sdk.exception;

/**
 * 请求限流异常（429）
 */
public class SqbRateLimitException extends SqbException {

    private final Long retryAfterMs;

    public SqbRateLimitException(String message, String requestId, Long retryAfterMs) {
        super(message, requestId, 429, true, "请求过于频繁，请稍后重试", null);
        this.retryAfterMs = retryAfterMs;
    }

    public Long getRetryAfterMs() {
        return retryAfterMs;
    }
}
