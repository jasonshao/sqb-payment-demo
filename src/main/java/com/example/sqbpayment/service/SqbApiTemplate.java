package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.exception.SqbException;
import com.example.sqbpayment.sdk.responsegetter.LiveSqbResponseGetter;
import com.example.sqbpayment.sdk.responsegetter.SqbApiRequest;
import com.example.sqbpayment.sdk.responsegetter.SqbResponseGetter;
import com.example.sqbpayment.sdk.transport.RestClientSqbTransport;
import com.example.sqbpayment.sdk.transport.SqbTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 收钱吧 API 调用模板
 *
 * 内部已委托给 SDK 层的 LiveSqbResponseGetter + RestClientSqbTransport。
 */
@Component
public class SqbApiTemplate {

    private final SqbConfig config;
    private final SqbResponseGetter responseGetter;

    @Autowired
    public SqbApiTemplate(SqbConfig config) {
        this.config = config;
        SqbTransport transport = new RestClientSqbTransport();
        this.responseGetter = new LiveSqbResponseGetter(transport, config.getApiBase());
    }

    SqbApiTemplate(SqbConfig config, SqbResponseGetter responseGetter) {
        this.config = config;
        this.responseGetter = responseGetter;
    }

    /**
     * 终端级别 API 调用（支付、查询、退款、签到）
     */
    public SqbResponse call(String path, Object request) throws SqbException {
        return responseGetter.request(new SqbApiRequest(
                path, request, config.getTerminalSn(), config.getTerminalKey()));
    }

    /**
     * 服务商级别 API 调用（终端激活）
     */
    public SqbResponse callAsVendor(String path, Object request) throws SqbException {
        return responseGetter.request(new SqbApiRequest(
                path, request, config.getVendorSn(), config.getVendorKey()));
    }
}
