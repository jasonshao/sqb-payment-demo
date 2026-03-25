package com.example.sqbpayment.sdk.webhook;

/**
 * 回调验签配置选项
 *
 * @param publicKey                  RSA 公钥（Base64 编码）
 * @param timestampToleranceSeconds  时间戳容忍秒数，默认 300 秒
 */
public record SqbWebhookOptions(
        String publicKey,
        int timestampToleranceSeconds
) {
    public SqbWebhookOptions(String publicKey) {
        this(publicKey, 300);
    }
}
