package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 订单查询服务，支持单次查询和轮询查询
 */
@Service
public class SqbQueryService {

    private static final Logger log = LoggerFactory.getLogger(SqbQueryService.class);
    private static final int POLL_TIMEOUT_SECONDS = 120;
    private static final int FAST_INTERVAL_MS = 3_000;
    private static final int SLOW_INTERVAL_MS = 10_000;
    private static final long FAST_PHASE_MS = 60_000L;

    private final SqbConfig config;
    private final SqbApiTemplate apiTemplate;

    public SqbQueryService(SqbConfig config, SqbApiTemplate apiTemplate) {
        this.config = config;
        this.apiTemplate = apiTemplate;
    }

    public SqbResponse queryByClientSn(String clientSn) {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        return apiTemplate.call("/upay/v2/query", request);
    }

    public SqbResponse queryBySn(String sn) {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(sn);
        return apiTemplate.call("/upay/v2/query", request);
    }

    @Async("pollExecutor")
    public CompletableFuture<SqbResponse> pollByClientSn(String clientSn) {
        return CompletableFuture.completedFuture(doPoll(clientSn, false));
    }

    @Async("pollExecutor")
    public CompletableFuture<SqbResponse> pollBySn(String sn) {
        return CompletableFuture.completedFuture(doPoll(sn, true));
    }

    SqbResponse doPoll(String identifier, boolean useSn) {
        long startNanos = System.nanoTime();
        long elapsedMs = 0;
        long timeoutMs = POLL_TIMEOUT_SECONDS * 1000L;

        while (elapsedMs < timeoutMs) {
            SqbResponse response = useSn ? queryBySn(identifier) : queryByClientSn(identifier);

            if (response.isCommunicationSuccess()) {
                String orderStatus = response.getOrderStatus();
                if (OrderStatus.isFinal(orderStatus)) {
                    log.info("轮询查询获得最终状态: {}={}, status={}", useSn ? "sn" : "clientSn", identifier, orderStatus);
                    return response;
                }
                log.info("订单状态未确定: {}={}, status={}, 继续轮询...", useSn ? "sn" : "clientSn", identifier, orderStatus);
            } else {
                log.warn("查询请求通信失败，继续轮询: {}={}", useSn ? "sn" : "clientSn", identifier);
            }

            int waitMs = elapsedMs < FAST_PHASE_MS ? FAST_INTERVAL_MS : SLOW_INTERVAL_MS;
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("轮询被中断: {}={}", useSn ? "sn" : "clientSn", identifier);
                return useSn ? queryBySn(identifier) : queryByClientSn(identifier);
            }
            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        }

        log.warn("轮询查询超时({}秒): {}={}，请人工确认", POLL_TIMEOUT_SECONDS, useSn ? "sn" : "clientSn", identifier);
        return useSn ? queryBySn(identifier) : queryByClientSn(identifier);
    }
}
