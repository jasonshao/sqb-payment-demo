package com.example.sqbpayment.sdk.exception;

/**
 * 网络连接异常
 */
public class SqbApiConnectionException extends SqbException {

    public SqbApiConnectionException(String message, Throwable cause) {
        super(message, null, null, true, "收钱吧服务通信失败", cause);
    }
}
