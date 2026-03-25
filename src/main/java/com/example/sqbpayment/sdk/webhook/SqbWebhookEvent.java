package com.example.sqbpayment.sdk.webhook;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 收钱吧回调通知事件，包含验签后解析的关键字段
 */
public record SqbWebhookEvent(
        String sn,
        String clientSn,
        String orderStatus,
        String totalAmount,
        JsonNode rawPayload
) {
}
