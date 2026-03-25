package com.example.sqbpayment.sdk.webhook;

import com.example.sqbpayment.sdk.exception.SqbSignatureVerificationException;
import com.example.sqbpayment.sdk.signing.SqbRsaUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 回调通知验签与解析的默认实现
 *
 * 流程：
 * 1. 校验 Authorization 头格式
 * 2. 提取签名并使用 RSA 公钥验签
 * 3. 解析 JSON 负载，提取关键业务字段
 */
@Component
public class DefaultSqbWebhookOperations implements SqbWebhookOperations {

    private static final Logger log = LoggerFactory.getLogger(DefaultSqbWebhookOperations.class);

    private final ObjectMapper objectMapper;

    public DefaultSqbWebhookOperations(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SqbWebhookEvent verifyAndParse(String payload, String authorization, SqbWebhookOptions options) {
        // 1. Validate authorization header format
        if (authorization == null || !authorization.contains(" ")) {
            throw new SqbSignatureVerificationException("Authorization 头格式无效，缺少空格分隔符");
        }

        // 2. Extract signature (content after the last space)
        String signature = authorization.substring(authorization.lastIndexOf(' ') + 1);

        // 3. Verify RSA signature
        boolean valid = SqbRsaUtil.verifySign(payload, options.publicKey(), signature);
        if (!valid) {
            throw new SqbSignatureVerificationException("回调通知签名验证失败");
        }

        log.info("回调通知签名验证通过");

        // 4. Parse JSON payload and extract fields
        try {
            JsonNode root = objectMapper.readTree(payload);

            String sn = root.path("sn").asText(null);
            String clientSn = root.path("client_sn").asText(null);

            // order_status with fallback to status
            String orderStatus = root.path("order_status").asText(null);
            if (orderStatus == null || orderStatus.isEmpty()) {
                orderStatus = root.path("status").asText(null);
            }

            String totalAmount = root.path("total_amount").asText(null);

            return new SqbWebhookEvent(sn, clientSn, orderStatus, totalAmount, root);

        } catch (Exception e) {
            throw new SqbSignatureVerificationException("回调通知负载解析失败: " + e.getMessage());
        }
    }
}
