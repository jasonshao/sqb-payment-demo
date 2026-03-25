package com.example.sqbpayment.sdk.transport;

import java.util.Map;

/**
 * 原始 HTTP 请求
 */
public record SqbRawRequest(
        String url,
        String body,
        Map<String, String> headers,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
