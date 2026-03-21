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

    /**
     * 发起退款
     *
     * @param command 退款命令（已通过 Bean Validation 校验）
     * @return 退款结果（可能经过轮询）
     */
    public CompletableFuture<SqbResponse> refund(RefundCommand command) throws IOException, InterruptedException {
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

        // REFUND_IN_PROGRESS / REFUND_FAIL_ERROR -> 异步轮询查询
        log.info("退款状态未确定，启动异步轮询: refundRequestNo={}", refundRequestNo);
        if (command.sn() != null && !command.sn().isEmpty()) {
            return queryService.pollBySn(command.sn());
        }
        return queryService.pollByClientSn(command.clientSn());
    }
}
