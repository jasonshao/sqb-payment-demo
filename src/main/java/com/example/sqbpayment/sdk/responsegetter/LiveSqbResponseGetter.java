package com.example.sqbpayment.sdk.responsegetter;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.SqbRequestOptions;
import com.example.sqbpayment.sdk.exception.*;
import com.example.sqbpayment.sdk.signing.SqbSignUtil;
import com.example.sqbpayment.sdk.transport.SqbRawRequest;
import com.example.sqbpayment.sdk.transport.SqbRawResponse;
import com.example.sqbpayment.sdk.transport.SqbTransport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认响应获取器实现
 *
 * 处理链路：合并 options -> 序列化 -> 签名 -> transport -> 反序列化 -> 异常映射
 */
public class LiveSqbResponseGetter implements SqbResponseGetter {

    private final SqbTransport transport;
    private final ObjectMapper objectMapper;
    private final String apiBase;
    private final SqbRequestOptions defaultOptions;

    public LiveSqbResponseGetter(SqbTransport transport, String apiBase, SqbRequestOptions defaultOptions) {
        this.transport = transport;
        this.objectMapper = new ObjectMapper();
        this.apiBase = apiBase;
        this.defaultOptions = defaultOptions;
    }

    public LiveSqbResponseGetter(SqbTransport transport, String apiBase) {
        this(transport, apiBase, null);
    }

    @Override
    public SqbResponse request(SqbApiRequest apiRequest) throws SqbException {
        SqbRequestOptions options = SqbRequestOptions.merge(defaultOptions, apiRequest.options());

        String body;
        try {
            body = objectMapper.writeValueAsString(apiRequest.body());
        } catch (Exception e) {
            throw new SqbException("请求序列化失败", e);
        }

        String url = apiBase + apiRequest.path();
        String authorization = SqbSignUtil.buildAuthorization(apiRequest.sn(), body, apiRequest.key());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        if (options != null && options.getIdempotencyKey() != null) {
            headers.put("X-Idempotency-Key", options.getIdempotencyKey());
        }
        if (options != null) {
            headers.putAll(options.getAdditionalHeaders());
        }

        int connectTimeout = (options != null && options.getConnectTimeoutMs() != null)
                ? options.getConnectTimeoutMs() : 0;
        int readTimeout = (options != null && options.getReadTimeoutMs() != null)
                ? options.getReadTimeoutMs() : 0;

        SqbRawRequest rawRequest = new SqbRawRequest(url, body, headers, connectTimeout, readTimeout);
        SqbRawResponse rawResponse = transport.execute(rawRequest);

        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(rawResponse.body());
        } catch (Exception e) {
            throw new SqbDeserializationException("响应 JSON 解析失败", e, rawResponse.body());
        }

        return new SqbResponse(responseNode);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
