package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.util.SqbHttpClient;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 收钱吧 API 调用模板
 *
 * 封装：序列化请求 → HTTP 签名调用 → 解析为 SqbResponse 的完整链路。
 * 所有对收钱吧 API 的调用都应通过此模板，确保签名、日志、错误处理的一致性。
 */
@Component
public class SqbApiTemplate {

    private final SqbHttpClient httpClient;
    private final SqbConfig config;

    public SqbApiTemplate(SqbHttpClient httpClient, SqbConfig config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    /**
     * 终端级别 API 调用（支付、查询、退款、签到）
     * 使用 terminal_sn + terminal_key 签名
     */
    public SqbResponse call(String path, Object request) throws IOException {
        return doCall(path, request, config.getTerminalSn(), config.getTerminalKey());
    }

    /**
     * 服务商级别 API 调用（终端激活）
     * 使用 vendor_sn + vendor_key 签名
     */
    public SqbResponse callAsVendor(String path, Object request) throws IOException {
        return doCall(path, request, config.getVendorSn(), config.getVendorKey());
    }

    private SqbResponse doCall(String path, Object request, String sn, String key) throws IOException {
        String body = httpClient.getObjectMapper().writeValueAsString(request);
        String url = config.getApiBase() + path;
        return new SqbResponse(httpClient.execute(url, body, sn, key));
    }
}
