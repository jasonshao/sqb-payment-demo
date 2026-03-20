package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.PayRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import com.example.sqbpayment.util.SqbHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 付款码支付服务（B扫C）
 *
 * 核心流程：
 * 1. 收银系统组装请求参数
 * 2. 计算签名，POST 到 /upay/v2/pay
 * 3. 解析三层响应：通信层 -> 业务层 -> 订单状态
 * 4. 非最终状态时启动轮询查询
 *
 * 注意事项：
 * - total_amount 单位为分（1元=100分）
 * - client_sn 必须全局唯一，支付失败后不能复用
 * - 只有 PAID 和 PAY_CANCELED 是最终状态
 */
@Service
public class SqbPayService {

    private static final Logger log = LoggerFactory.getLogger(SqbPayService.class);

    private final SqbConfig config;
    private final SqbHttpClient httpClient;
    private final SqbQueryService queryService;

    public SqbPayService(SqbConfig config, SqbHttpClient httpClient, SqbQueryService queryService) {
        this.config = config;
        this.httpClient = httpClient;
        this.queryService = queryService;
    }

    /**
     * 发起付款码支付
     *
     * @param dynamicId   顾客付款码内容（扫码枪扫描获得）
     * @param totalAmount 金额，单位为分
     * @param subject     交易简介，显示在顾客账单中
     * @param operator    操作员
     * @param notifyUrl   异步回调通知地址（可选）
     * @return 支付结果（可能经过轮询）
     */
    public CompletableFuture<SqbResponse> pay(String dynamicId, String totalAmount, String subject,
                                                String operator, String notifyUrl) throws IOException, InterruptedException {

        String clientSn = ClientSnGenerator.generate();

        PayRequest request = new PayRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        request.setTotalAmount(totalAmount);
        request.setDynamicId(dynamicId);
        request.setSubject(subject);
        request.setOperator(operator);
        request.setNotifyUrl(notifyUrl);

        String requestBody = httpClient.getObjectMapper().writeValueAsString(request);
        String url = config.getApiBase() + "/upay/v2/pay";

        JsonNode response = httpClient.execute(url, requestBody, config.getTerminalSn(), config.getTerminalKey());
        SqbResponse sqbResponse = new SqbResponse(response);

        // 三层响应判定
        if (!sqbResponse.isCommunicationSuccess()) {
            log.error("支付请求通信失败: {}", sqbResponse);
            return CompletableFuture.completedFuture(sqbResponse);
        }

        String bizResultCode = sqbResponse.getBizResultCode();

        if ("PAY_FAIL".equals(bizResultCode)) {
            log.info("支付失败: clientSn={}, reason={}", clientSn, sqbResponse.getBizErrorMessage());
            return CompletableFuture.completedFuture(sqbResponse);
        }

        if ("PAY_SUCCESS".equals(bizResultCode)) {
            String orderStatus = sqbResponse.getOrderStatus();
            if (OrderStatus.isFinal(orderStatus)) {
                log.info("支付结果确定: clientSn={}, status={}", clientSn, orderStatus);
                return CompletableFuture.completedFuture(sqbResponse);
            }
        }

        // PAY_IN_PROGRESS / PAY_FAIL_ERROR / 非最终状态 -> 异步轮询（不阻塞请求线程）
        log.info("支付状态未确定，启动异步轮询查询: clientSn={}, bizResultCode={}", clientSn, bizResultCode);
        return queryService.pollByClientSn(clientSn);
    }
}
