package com.example.sqbpayment.domain.polling;

import com.example.sqbpayment.infrastructure.config.SqbPollingProperties;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 通用轮询服务，从 SqbQueryService 中提取的轮询逻辑
 *
 * 使用两阶段轮询策略：
 * - 快速阶段（前半段超时时间内）：使用 fastIntervalMs 间隔
 * - 慢速阶段（后半段超时时间内）：使用 slowIntervalMs 间隔
 *
 * 当响应达到最终状态或超时时返回最后一次查询结果
 */
@Component
public class PollingService {

    private static final Logger log = LoggerFactory.getLogger(PollingService.class);

    private final SqbPollingProperties pollingProperties;
    private final Executor pollExecutor;

    public PollingService(SqbPollingProperties pollingProperties,
                          @Qualifier("pollExecutor") Executor pollExecutor) {
        this.pollingProperties = pollingProperties;
        this.pollExecutor = pollExecutor;
    }

    /**
     * 异步轮询直到响应达到最终状态或超时
     *
     * @param queryAction 每次轮询执行的查询动作
     * @return 包含最终（或超时时最后一次）响应的 CompletableFuture
     */
    public CompletableFuture<SqbResponse> pollUntilFinal(Supplier<SqbResponse> queryAction) {
        if (!pollingProperties.isEnabled()) {
            log.info("轮询已禁用，执行单次查询");
            return CompletableFuture.supplyAsync(queryAction, pollExecutor);
        }

        return CompletableFuture.supplyAsync(() -> doPoll(queryAction), pollExecutor);
    }

    private SqbResponse doPoll(Supplier<SqbResponse> queryAction) {
        long startNanos = System.nanoTime();
        long timeoutMs = pollingProperties.getTimeoutMs();
        long fastPhaseMs = timeoutMs / 2;
        long elapsedMs = 0;

        while (elapsedMs < timeoutMs) {
            SqbResponse response = queryAction.get();

            if (response.isCommunicationSuccess()) {
                String orderStatus = response.getOrderStatus();
                if (OrderStatus.isFinal(orderStatus)) {
                    log.info("轮询查询获得最终状态: status={}", orderStatus);
                    return response;
                }
                log.info("订单状态未确定: status={}, 继续轮询...", orderStatus);
            } else {
                log.warn("查询请求通信失败，继续轮询");
            }

            int waitMs = elapsedMs < fastPhaseMs
                    ? pollingProperties.getFastIntervalMs()
                    : pollingProperties.getSlowIntervalMs();

            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("轮询被中断，执行最后一次查询");
                return queryAction.get();
            }

            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        }

        log.warn("轮询查询超时({}ms)，请人工确认", timeoutMs);
        return queryAction.get();
    }
}
