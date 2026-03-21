package com.example.sqbpayment.controller;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.util.SqbSignUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 异步回调通知处理器
 *
 * 收钱吧会在交易状态变化时推送通知，重试策略：1s, 5s, 30s, 600s（共4次重试）
 *
 * 注意事项：
 * - 必须验证签名，防止伪造请求
 * - 必须实现幂等处理（同一订单可能收到多次通知）
 * - 成功处理后返回纯文本 "success"
 * - 回调通知不能替代主动轮询查询
 */
@RestController
@RequestMapping("/api/notify")
public class SqbNotifyController {

    private static final Logger log = LoggerFactory.getLogger(SqbNotifyController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_CACHE_SIZE = 10_000;

    /** 需要关注的最终状态 */
    private static final Set<String> FINAL_STATUSES = Set.of(
            "PAID", "PAY_CANCELED", "REFUNDED", "PARTIAL_REFUNDED", "CANCELED"
    );

    /** 幂等记录：有界 LRU 缓存，防止无限增长导致内存泄漏 */
    private final Map<String, Long> processedOrders = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private final SqbConfig config;

    public SqbNotifyController(SqbConfig config) {
        this.config = config;
    }

    /**
     * 接收收钱吧异步回调通知
     * 返回纯文本 "success" 表示接收成功，否则收钱吧会重试
     */
    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleNotify(HttpServletRequest request) throws IOException {
        // 读取请求体
        String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("收到收钱吧回调通知: {}", requestBody);

        // 验证签名
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.contains(" ")) {
            log.warn("回调通知签名缺失");
            return "fail";
        }

        String[] parts = authorization.split(" ", 2);
        String receivedSn = parts[0];
        String receivedSign = parts[1];

        if (!SqbSignUtil.verifySign(requestBody, config.getTerminalKey(), receivedSign)) {
            log.warn("回调通知签名验证失败: sn={}", receivedSn);
            return "fail";
        }

        // 解析通知内容
        JsonNode notification = OBJECT_MAPPER.readTree(requestBody);
        String clientSn = notification.path("client_sn").asText("");
        String orderStatus = notification.path("order_status").asText(notification.path("status").asText(""));
        String sn = notification.path("sn").asText("");

        log.info("回调通知解析: sn={}, clientSn={}, orderStatus={}", sn, clientSn, orderStatus);

        // 幂等处理：使用 sn 作为去重键，putIfAbsent 保证原子性
        String deduplicationKey = sn.isEmpty() ? clientSn : sn;
        if (!deduplicationKey.isEmpty()) {
            Long previous = processedOrders.putIfAbsent(deduplicationKey, System.currentTimeMillis());
            if (previous != null) {
                log.info("回调通知重复，已忽略: key={}, orderStatus={}", deduplicationKey, orderStatus);
                return "success";
            }
        }

        if (FINAL_STATUSES.contains(orderStatus)) {
            log.info("订单到达最终状态: clientSn={}, status={}", clientSn, orderStatus);
            // TODO: 更新本地订单状态
        }

        // 返回 "success" 告知收钱吧已成功处理
        return "success";
    }
}
