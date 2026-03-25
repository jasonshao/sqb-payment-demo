package com.example.sqbpayment.sdk.exception;

/**
 * 认证/鉴权异常（401/403 或签名错误）
 */
public class SqbAuthenticationException extends SqbException {

    public SqbAuthenticationException(String message, String requestId, Integer httpStatus) {
        super(message, requestId, httpStatus, false, "认证失败", null);
    }
}
