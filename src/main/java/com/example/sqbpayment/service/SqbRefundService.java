package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.RefundRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import com.example.sqbpayment.util.SqbHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 退款服务
 *
 * 注意事项：
 * - 退款是异步操作，提交后需轮询确认最终结果
 * - refund_request_no 必须唯一
 * - refund_amount 单位为分
 * - 累计退款金额不能超过原订单金额
 * - 支持全额退款和部分退款
 */
@Service
public class SqbRefundService {

    private static final Logger log = LoggerFactory.getLogger(SqbRefundService.class);

    private final SqbConfig config;
    private final SqbHttpClient httpClient;
    private final SqbQueryService queryService;
    private final ClientSnGenerator clientSnGenerator;

    public SqbRefundService(SqbConfig config, SqbHttpClient httpClient, SqbQueryService queryService,
                            ClientSnGenerator clientSnGenerator) {
        this.config = config;
        this.httpClient = httpClient;
        this.queryService = queryService;
        this.clientSnGenerator = clientSnGenerator;
    }

    /**
     * 发起退款
     *
     * @param sn           收钱吧订单号（与 clientSn 二选一）
     * @param clientSn     商户订单号（与 sn 二选一）
     * @param refundAmount 退款金额，单位为分
     * @param operator     操作员
     * @param refundReason 退款原因（可选）
     * @return 退款结果（可能经过轮询）
     */
    public CompletableFuture<SqbResponse> refund(String sn, String clientSn, String refundAmount,
                                                    String operator, String refundReason) throws IOException, InterruptedException {

        String refundRequestNo = clientSnGenerator.generateRefundNo();

        RefundRequest request = new RefundRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(sn);
        request.setClientSn(clientSn);
        request.setRefundRequestNo(refundRequestNo);
        request.setRefundAmount(refundAmount);
        request.setOperator(operator);
        request.setRefundReason(refundReason);

        String requestBody = httpClient.getObjectMapper().writeValueAsString(request);
        String url = config.getApiBase() + "/upay/v2/refund";

        JsonNode response = httpClient.execute(url, requestBody, config.getTerminalSn(), config.getTerminalKey());
        SqbResponse sqbResponse = new SqbResponse(response);

        if (!sqbResponse.isCommunicationSuccess()) {
            log.error("退款请求通信失败: {}", sqbResponse);
            return CompletableFuture.completedFuture(sqbResponse);
        }

        String bizResultCode = sqbResponse.getBizResultCode();

        if ("REFUND_SUCCESS".equals(bizResultCode)) {
            String orderStatus = sqbResponse.getOrderStatus();
            if (OrderStatus.isFinal(orderStatus)) {
                log.info("退款成功: refundRequestNo={}, status={}", refundRequestNo, orderStatus);
                return CompletableFuture.completedFuture(sqbResponse);
            }
        }

        if ("REFUND_FAIL".equals(bizResultCode)) {
            log.info("退款失败: refundRequestNo={}, reason={}", refundRequestNo, sqbResponse.getBizErrorMessage());
            return CompletableFuture.completedFuture(sqbResponse);
        }

        // REFUND_IN_PROGRESS / REFUND_FAIL_ERROR -> 异步轮询查询
        log.info("退款状态未确定，启动异步轮询: refundRequestNo={}", refundRequestNo);
        if (sn != null && !sn.isEmpty()) {
            return queryService.pollBySn(sn);
        }
        return queryService.pollByClientSn(clientSn);
    }
}
