package com.example.sqbpayment.sdk.webhook;

/**
 * 回调通知验签与解析操作接口
 */
public interface SqbWebhookOperations {

    /**
     * 验证回调签名并解析通知内容
     *
     * @param payload       回调请求体原文
     * @param authorization 回调请求的 Authorization 头
     * @param options       验签配置（公钥等）
     * @return 解析后的回调事件
     * @throws com.example.sqbpayment.sdk.exception.SqbSignatureVerificationException 签名验证失败
     */
    SqbWebhookEvent verifyAndParse(String payload, String authorization, SqbWebhookOptions options);
}
