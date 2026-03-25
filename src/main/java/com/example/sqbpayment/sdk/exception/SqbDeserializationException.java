package com.example.sqbpayment.sdk.exception;

/**
 * 响应反序列化异常
 */
public class SqbDeserializationException extends SqbException {

    private final String rawBody;

    public SqbDeserializationException(String message, Throwable cause, String rawBody) {
        super(message, null, null, false, "响应解析失败", cause);
        this.rawBody = rawBody;
    }

    public String getRawBody() {
        return rawBody;
    }
}
