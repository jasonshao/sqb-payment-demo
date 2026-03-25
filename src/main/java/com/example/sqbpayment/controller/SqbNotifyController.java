package com.example.sqbpayment.controller;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.domain.order.IdempotencyRecord;
import com.example.sqbpayment.domain.order.IdempotencyRepository;
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

    /** 需要关注的最终状态 */
    private static final Set<String> FINAL_STATUSES = Set.of(
            "PAID", "PAY_CANCELED", "REFUNDED", "PARTIAL_REFUNDED", "CANCELED"
    );

    private final SqbConfig config;
    private final IdempotencyRepository idempotencyRepository;

    public SqbNotifyController(SqbConfig config, IdempotencyRepository idempotencyRepository) {
        this.config = config;
        this.idempotencyRepository = idempotencyRepository;
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

        // 幂等处理：使用 DB 记录去重
        String deduplicationKey = sn.isEmpty() ? clientSn : sn;
        if (!deduplicationKey.isEmpty()) {
            if (idempotencyRepository.existsByIdempotencyKey(deduplicationKey)) {
                log.info("回调通知重复，已忽略: key={}, orderStatus={}", deduplicationKey, orderStatus);
                return ResponseEntity.ok("success");
            }
            idempotencyRepository.save(new IdempotencyRecord(deduplicationKey, orderStatus));
        }

        if (FINAL_STATUSES.contains(orderStatus)) {
            log.info("订单到达最终状态: clientSn={}, status={}", clientSn, orderStatus);
        }

        return ResponseEntity.ok("success");
    }

}
