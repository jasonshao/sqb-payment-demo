package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.QueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 订单查询服务，支持单次查询和轮询查询
 *
 * 轮询策略：
 * - 0~60秒：每3秒查询一次
 * - 60秒~超时：每10秒查询一次
 * - 默认超时：120秒
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

    /**
     * 单次查询订单（通过商户订单号）
     */
    public SqbResponse queryByClientSn(String clientSn) throws IOException {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        return apiTemplate.call("/upay/v2/query", request);
    }

    /**
     * 单次查询订单（通过收钱吧订单号）
     */
    public SqbResponse queryBySn(String sn) throws IOException {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(sn);
        return apiTemplate.call("/upay/v2/query", request);
    }

    /**
     * 异步轮询查询直到获得最终状态（通过商户订单号）
     */
    @Async("pollExecutor")
    public CompletableFuture<SqbResponse> pollByClientSn(String clientSn) throws IOException, InterruptedException {
        return CompletableFuture.completedFuture(doPoll(clientSn, false));
    }

    /**
     * 异步轮询查询直到获得最终状态（通过收钱吧订单号）
     */
    @Async("pollExecutor")
    public CompletableFuture<SqbResponse> pollBySn(String sn) throws IOException, InterruptedException {
        return CompletableFuture.completedFuture(doPoll(sn, true));
    }

    /**
     * 统一轮询实现，消除 doPollByClientSn / doPollBySn 的重复代码
     */
    SqbResponse doPoll(String identifier, boolean useSn) throws IOException, InterruptedException {
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
            Thread.sleep(waitMs);
            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        }

        // 超时，返回最后一次查询结果
        log.warn("轮询查询超时({}秒): {}={}，请人工确认", POLL_TIMEOUT_SECONDS, useSn ? "sn" : "clientSn", identifier);
        return useSn ? queryBySn(identifier) : queryByClientSn(identifier);
    }
}
