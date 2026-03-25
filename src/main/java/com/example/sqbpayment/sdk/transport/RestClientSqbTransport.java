package com.example.sqbpayment.sdk.transport;

import com.example.sqbpayment.sdk.exception.SqbApiConnectionException;
import com.example.sqbpayment.sdk.exception.SqbApiTimeoutException;
import com.example.sqbpayment.sdk.exception.SqbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于 Spring RestClient 的传输层实现
 *
 * 特性：
 * - 按请求级超时控制
 * - 网络重试（指数退避 + jitter）
 * - 脱敏 INFO 日志
 */
public class RestClientSqbTransport implements SqbTransport {

    private static final Logger log = LoggerFactory.getLogger(RestClientSqbTransport.class);

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 60_000;
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final long BASE_BACKOFF_MS = 500;

    private final int maxRetries;

    public RestClientSqbTransport() {
        this(DEFAULT_MAX_RETRIES);
    }

    public RestClientSqbTransport(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public SqbRawResponse execute(SqbRawRequest request) throws SqbException {
        int attempts = 0;
        SqbException lastException = null;

        while (attempts <= maxRetries) {
            try {
                return doExecute(request);
            } catch (SqbApiConnectionException e) {
                lastException = e;
                attempts++;
                if (attempts > maxRetries) {
                    break;
                }
                long backoff = calculateBackoff(attempts);
                log.warn("收钱吧请求网络失败，第 {} 次重试 ({}ms 后): URL={}", attempts, backoff, request.url());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SqbApiConnectionException("重试等待被中断", ie);
                }
            }
        }

        throw lastException;
    }

    private SqbRawResponse doExecute(SqbRawRequest request) throws SqbException {
        int connectTimeout = request.connectTimeoutMs() > 0 ? request.connectTimeoutMs() : DEFAULT_CONNECT_TIMEOUT_MS;
        int readTimeout = request.readTimeoutMs() > 0 ? request.readTimeoutMs() : DEFAULT_READ_TIMEOUT_MS;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        log.info("收钱吧请求: URL={}", request.url());

        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(request.url())
                    .contentType(MediaType.APPLICATION_JSON);

            for (Map.Entry<String, String> header : request.headers().entrySet()) {
                spec.header(header.getKey(), header.getValue());
            }

            String responseBody = spec.body(request.body())
                    .retrieve()
                    .body(String.class);

            log.info("收钱吧响应: URL={}, bodyLength={}", request.url(),
                    responseBody != null ? responseBody.length() : 0);

            return new SqbRawResponse(200, responseBody, Collections.emptyMap());

        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new SqbApiTimeoutException(
                        "请求超时: " + request.url(),
                        e,
                        connectTimeout,
                        readTimeout);
            }
            throw new SqbApiConnectionException("网络连接失败: " + request.url(), e);
        } catch (Exception e) {
            if (e instanceof SqbException sqbEx) {
                throw sqbEx;
            }
            throw new SqbApiConnectionException("请求异常: " + request.url(), e);
        }
    }

    private long calculateBackoff(int attempt) {
        long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, backoff / 2 + 1);
        return backoff + jitter;
    }
}
