package com.example.sqbpayment.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 收钱吧 HTTP 客户端（基于 Spring RestClient）
 *
 * 特性：
 * - 使用 Spring 6.1 内置 RestClient，无需第三方 HTTP 依赖
 * - 签名通过拦截器自动注入 Authorization 头
 * - 请求/响应自动序列化
 */
@Component
public class SqbHttpClient {

    private static final Logger log = LoggerFactory.getLogger(SqbHttpClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public SqbHttpClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);

        this.objectMapper = new ObjectMapper();
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    /**
     * 测试专用构造器：允许注入自定义 RestClient
     */
    SqbHttpClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
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

        log.info("收钱吧请求: URL={}", url);
        log.debug("收钱吧请求详情: URL={}, Body={}", url, requestBody);

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authorization)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        log.debug("收钱吧响应详情: Body={}", responseBody);

        JsonNode responseNode = objectMapper.readTree(responseBody);
        log.info("收钱吧响应: URL={}, result_code={}", url, responseNode.path("result_code").asText("N/A"));

        return responseNode;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
