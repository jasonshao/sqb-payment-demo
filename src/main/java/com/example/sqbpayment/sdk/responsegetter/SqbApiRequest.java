package com.example.sqbpayment.sdk.responsegetter;

import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * API 请求封装，包含路径、请求体、凭证和请求级配置
 */
public record SqbApiRequest(
        String path,
        Object body,
        String sn,
        String key,
        SqbRequestOptions options
) {
    public SqbApiRequest(String path, Object body, String sn, String key) {
        this(path, body, sn, key, null);
    }
}
