package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.CancelCommand;
import com.example.sqbpayment.model.request.CancelRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 撤单服务
 *
 * 注意事项：
 * - 撤单用于未完成的交易（如支付中、支付异常）
 * - CANCEL_ERROR 必须通过查询确认最终状态
 * - 支持通过 sn 或 clientSn 撤单
 */
@Service
public class SqbCancelService {

    private static final Logger log = LoggerFactory.getLogger(SqbCancelService.class);

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;
    private final SqbQueryService queryService;

    public SqbCancelService(SqbConfig config, SqbApiTemplate apiTemplate, SqbQueryService queryService) {
        this.config = config;
        this.apiTemplate = apiTemplate;
        this.queryService = queryService;
    }

    /**
     * 发起撤单
     *
     * @param command 撤单命令
     * @return 撤单结果（可能经过轮询）
     */
    public CompletableFuture<SqbResponse> cancel(CancelCommand command) throws IOException, InterruptedException {
        command.validate();

        CancelRequest request = new CancelRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(command.sn());
        request.setClientSn(command.clientSn());

        SqbResponse sqbResponse = apiTemplate.call("/upay/v2/cancel", request);

        if (!sqbResponse.isCommunicationSuccess()) {
            log.error("撤单请求通信失败: {}", sqbResponse);
            return CompletableFuture.completedFuture(sqbResponse);
        }

        String bizResultCode = sqbResponse.getBizResultCode();

        if ("CANCEL_SUCCESS".equals(bizResultCode)) {
            String orderStatus = sqbResponse.getOrderStatus();
            if (OrderStatus.isFinal(orderStatus)) {
                log.info("撤单成功: status={}", orderStatus);
                return CompletableFuture.completedFuture(sqbResponse);
            }
        }

        if ("CANCEL_ERROR".equals(bizResultCode)) {
            log.warn("撤单结果不确定，启动查询确认: sn={}, clientSn={}", command.sn(), command.clientSn());
            return pollByIdentifier(command);
        }

        if ("CANCEL_FAIL".equals(bizResultCode)) {
            log.info("撤单失败: reason={}", sqbResponse.getBizErrorMessage());
            return CompletableFuture.completedFuture(sqbResponse);
        }

        // 未知状态 -> 轮询
        log.info("撤单状态未确定，启动异步轮询: bizResultCode={}", bizResultCode);
        return pollByIdentifier(command);
    }

    private CompletableFuture<SqbResponse> pollByIdentifier(CancelCommand command) throws IOException, InterruptedException {
        if (command.sn() != null && !command.sn().isEmpty()) {
            return queryService.pollBySn(command.sn());
        }
        return queryService.pollByClientSn(command.clientSn());
    }
}
