package com.example.sqbpayment.sdk.transport;

import com.example.sqbpayment.sdk.exception.SqbException;

/**
 * HTTP 传输层接口
 *
 * 职责：发送 HTTP 请求、超时控制、网络重试、脱敏日志
 */
public interface SqbTransport {

    SqbRawResponse execute(SqbRawRequest request) throws SqbException;
}
