package com.example.sqbpayment.sdk.exception;

/**
 * 签名验证失败异常（用于回调验签）
 */
public class SqbSignatureVerificationException extends SqbException {

    public SqbSignatureVerificationException(String message) {
        super(message, null, null, false, "签名验证失败", null);
    }
}
