package com.example.sqbpayment.controller;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.sdk.signing.SqbRsaUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final long TTL_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours

    /** 需要关注的最终状态 */
    private static final Set<String> FINAL_STATUSES = Set.of(
            "PAID", "PAY_CANCELED", "REFUNDED", "PARTIAL_REFUNDED", "CANCELED"
    );

    /** 幂等记录：ConcurrentHashMap + TTL，超过 24 小时的条目会被清理 */
    private final ConcurrentHashMap<String, Long> processedOrders = new ConcurrentHashMap<>();

    private final SqbConfig config;

    public SqbNotifyController(SqbConfig config) {
        this.config = config;
    }

    /**
     * 接收收钱吧异步回调通知
     * 返回纯文本 "success" 表示接收成功，验签失败返回 HTTP 403
     */
    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleNotify(HttpServletRequest request) throws IOException {
        // 读取请求体
        String requestBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("收到收钱吧回调通知: {}", requestBody);

        // 验证 RSA 签名
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.contains(" ")) {
            log.warn("回调通知签名缺失");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("signature verification failed");
        }

        String[] parts = authorization.split(" ", 2);
        String receivedSn = parts[0];
        String receivedSign = parts[1];

        String publicKey = config.getNotifyPublicKey();
        if (publicKey == null || publicKey.isBlank()
                || !SqbRsaUtil.verifySign(requestBody, publicKey, receivedSign)) {
            log.warn("回调通知签名验证失败: sn={}", receivedSn);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("signature verification failed");
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
            long now = System.currentTimeMillis();
            Long previous = processedOrders.putIfAbsent(deduplicationKey, now);
            if (previous != null) {
                log.info("回调通知重复，已忽略: key={}, orderStatus={}", deduplicationKey, orderStatus);
                return ResponseEntity.ok("success");
            }
            // 当缓存超过上限时，清理过期条目（超过 24 小时）
            if (processedOrders.size() > MAX_CACHE_SIZE) {
                cleanExpiredEntries(now);
            }
        }

        if (FINAL_STATUSES.contains(orderStatus)) {
            log.info("订单到达最终状态: clientSn={}, status={}", clientSn, orderStatus);
        }

        return ResponseEntity.ok("success");
    }

    /**
     * 清理过期的幂等记录（超过 TTL 的条目）
     */
    private void cleanExpiredEntries(long now) {
        Iterator<Map.Entry<String, Long>> it = processedOrders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (now - entry.getValue() > TTL_MILLIS) {
                it.remove();
            }
        }
    }
}
