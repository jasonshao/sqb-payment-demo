package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.RefundCommand;
import com.example.sqbpayment.model.request.RefundRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 退款服务
 */
@Service
public class SqbRefundService {

    private static final Logger log = LoggerFactory.getLogger(SqbRefundService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;
    private final SqbQueryService queryService;
    private final ClientSnGenerator clientSnGenerator;

    public SqbRefundService(SqbConfig config, SqbApiTemplate apiTemplate, SqbQueryService queryService,
                            ClientSnGenerator clientSnGenerator) {
        this.config = config;
        this.apiTemplate = apiTemplate;
        this.queryService = queryService;
        this.clientSnGenerator = clientSnGenerator;
    }

    public CompletableFuture<SqbResponse> refund(RefundCommand command) {
        command.validate();

        String refundRequestNo = clientSnGenerator.generateRefundNo();

        RefundRequest request = new RefundRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(command.sn());
        request.setClientSn(command.clientSn());
        request.setRefundRequestNo(refundRequestNo);
        request.setRefundAmount(String.valueOf(command.refundAmount()));
        request.setOperator(command.operator());
        request.setRefundReason(command.refundReason());

        SqbResponse sqbResponse = apiTemplate.call("/upay/v2/refund", request);

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

        log.info("退款状态未确定，启动异步轮询: refundRequestNo={}", refundRequestNo);
        if (command.sn() != null && !command.sn().isEmpty()) {
            return queryService.pollBySn(command.sn());
        }
        return queryService.pollByClientSn(command.clientSn());
    }
}
