package com.example.sqbpayment.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求级配置，支持"全局默认 + 单次覆盖"模式
 */
public class SqbRequestOptions {

    private final String idempotencyKey;
    private final Integer connectTimeoutMs;
    private final Integer readTimeoutMs;
    private final Integer maxNetworkRetries;
    private final String notifyUrlOverride;
    private final Map<String, String> additionalHeaders;

    private SqbRequestOptions(Builder builder) {
        this.idempotencyKey = builder.idempotencyKey;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.maxNetworkRetries = builder.maxNetworkRetries;
        this.notifyUrlOverride = builder.notifyUrlOverride;
        this.additionalHeaders = builder.additionalHeaders.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(builder.additionalHeaders));
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public Integer getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public Integer getMaxNetworkRetries() {
        return maxNetworkRetries;
    }

    public String getNotifyUrlOverride() {
        return notifyUrlOverride;
    }

    public Map<String, String> getAdditionalHeaders() {
        return additionalHeaders;
    }

    /**
     * 合并：per-request 覆盖 defaults（per-request 字段非 null 时优先）
     */
    public static SqbRequestOptions merge(SqbRequestOptions defaults, SqbRequestOptions perRequest) {
        if (perRequest == null) return defaults;
        if (defaults == null) return perRequest;

        Builder builder = builder();
        builder.setIdempotencyKey(perRequest.idempotencyKey != null ? perRequest.idempotencyKey : defaults.idempotencyKey);
        builder.setConnectTimeoutMs(perRequest.connectTimeoutMs != null ? perRequest.connectTimeoutMs : defaults.connectTimeoutMs);
        builder.setReadTimeoutMs(perRequest.readTimeoutMs != null ? perRequest.readTimeoutMs : defaults.readTimeoutMs);
        builder.setMaxNetworkRetries(perRequest.maxNetworkRetries != null ? perRequest.maxNetworkRetries : defaults.maxNetworkRetries);
        builder.setNotifyUrlOverride(perRequest.notifyUrlOverride != null ? perRequest.notifyUrlOverride : defaults.notifyUrlOverride);

        Map<String, String> merged = new HashMap<>(defaults.additionalHeaders);
        merged.putAll(perRequest.additionalHeaders);
        builder.additionalHeaders = merged;

        return builder.build();
    }

    public static class Builder {
        private String idempotencyKey;
        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
        private Integer maxNetworkRetries;
        private String notifyUrlOverride;
        private Map<String, String> additionalHeaders = new HashMap<>();

        public Builder setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder setReadTimeoutMs(Integer readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder setMaxNetworkRetries(Integer maxNetworkRetries) {
            this.maxNetworkRetries = maxNetworkRetries;
            return this;
        }

        public Builder setNotifyUrlOverride(String notifyUrlOverride) {
            this.notifyUrlOverride = notifyUrlOverride;
            return this;
        }

        public Builder addHeader(String key, String value) {
            this.additionalHeaders.put(key, value);
            return this;
        }

        public SqbRequestOptions build() {
            return new SqbRequestOptions(this);
        }
    }
}
