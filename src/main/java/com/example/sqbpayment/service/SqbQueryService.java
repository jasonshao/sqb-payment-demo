package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.enums.OrderStatus;
import com.example.sqbpayment.model.request.QueryRequest;
import com.example.sqbpayment.util.SqbHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
    private static final int FAST_INTERVAL_MS = 3000;
    private static final int SLOW_INTERVAL_MS = 10000;
    private static final int FAST_PHASE_SECONDS = 60;

    private final SqbConfig config;
    private final SqbHttpClient httpClient;

    public SqbQueryService(SqbConfig config, SqbHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    /**
     * 单次查询订单（通过商户订单号）
     */
    public SqbResponse queryByClientSn(String clientSn) throws IOException {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setClientSn(clientSn);
        return doQuery(request);
    }

    /**
     * 单次查询订单（通过收钱吧订单号）
     */
    public SqbResponse queryBySn(String sn) throws IOException {
        QueryRequest request = new QueryRequest();
        request.setTerminalSn(config.getTerminalSn());
        request.setSn(sn);
        return doQuery(request);
    }

    /**
     * 轮询查询直到获得最终状态
     * 最终状态：PAID, PAY_CANCELED, REFUNDED, PARTIAL_REFUNDED, CANCELED
     *
     * @param clientSn 商户订单号
     * @return 最终状态的查询结果
     */
    public SqbResponse pollByClientSn(String clientSn) throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        long elapsed = 0;

        while (elapsed < POLL_TIMEOUT_SECONDS * 1000L) {
            SqbResponse response = queryByClientSn(clientSn);

            if (response.isCommunicationSuccess()) {
                String orderStatus = response.getOrderStatus();
                if (OrderStatus.isFinal(orderStatus)) {
                    log.info("轮询查询获得最终状态: clientSn={}, status={}", clientSn, orderStatus);
                    return response;
                }
                log.info("订单状态未确定: clientSn={}, status={}, 继续轮询...", clientSn, orderStatus);
            } else {
                log.warn("查询请求通信失败，继续轮询: clientSn={}", clientSn);
            }

            // 前60秒每3秒查询，之后每10秒查询
            int waitMs = elapsed < FAST_PHASE_SECONDS * 1000L ? FAST_INTERVAL_MS : SLOW_INTERVAL_MS;
            Thread.sleep(waitMs);
            elapsed = System.currentTimeMillis() - startTime;
        }

        // 超时，返回最后一次查询结果
        log.warn("轮询查询超时({}秒): clientSn={}，请人工确认", POLL_TIMEOUT_SECONDS, clientSn);
        return queryByClientSn(clientSn);
    }

    private SqbResponse doQuery(QueryRequest request) throws IOException {
        String requestBody = httpClient.getObjectMapper().writeValueAsString(request);
        String url = config.getApiBase() + "/upay/v2/query";

        JsonNode response = httpClient.execute(url, requestBody, config.getTerminalSn(), config.getTerminalKey());
        return new SqbResponse(response);
    }
}
