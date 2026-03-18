package com.example.sqbpayment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 收钱吧 HTTP 客户端封装
 */
@Component
public class SqbHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SqbHttpClient.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SqbHttpClient() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送签名请求到收钱吧 API
     *
     * @param url         完整的 API URL
     * @param requestBody JSON 请求体字符串（签名时用的同一份）
     * @param sn          vendor_sn 或 terminal_sn
     * @param key         vendor_key 或 terminal_key
     * @return 响应 JSON 节点
     */
    public JsonNode execute(String url, String requestBody, String sn, String key) throws IOException {
        String authorization = SqbSignUtil.buildAuthorization(sn, requestBody, key);

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .addHeader("Authorization", authorization)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        log.info("收钱吧请求: URL={}, Body={}", url, requestBody);

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("收钱吧响应: Status={}, Body={}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                throw new IOException("HTTP request failed with status: " + response.code() + ", body: " + responseBody);
            }

            return objectMapper.readTree(responseBody);
        }
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
