package com.example.sqbpayment.sdk.transport;

import java.util.Map;

/**
 * 原始 HTTP 响应
 */
public record SqbRawResponse(
        int statusCode,
        String body,
        Map<String, String> headers
) {
}
