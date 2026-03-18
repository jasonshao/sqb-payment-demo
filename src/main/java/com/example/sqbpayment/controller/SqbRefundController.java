package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbRefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 退款控制器
 */
@RestController
@RequestMapping("/api/refund")
public class SqbRefundController {

    private final SqbRefundService refundService;

    public SqbRefundController(SqbRefundService refundService) {
        this.refundService = refundService;
    }

    /**
     * 发起退款
     *
     * 请求参数：
     * - sn: 收钱吧订单号（与 clientSn 二选一）
     * - clientSn: 商户订单号（与 sn 二选一）
     * - refundAmount: 退款金额，单位为分（必填）
     * - operator: 操作员（必填）
     * - refundReason: 退款原因（选填）
     *
     * 退款为异步操作，该接口会自动轮询等待最终结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> refund(@RequestBody Map<String, String> params) {
        String sn = params.get("sn");
        String clientSn = params.get("clientSn");
        String refundAmount = params.get("refundAmount");
        String operator = params.get("operator");
        String refundReason = params.get("refundReason");

        if (refundAmount == null || operator == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "缺少必填参数：refundAmount, operator"
            ));
        }
        if (sn == null && clientSn == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "需要提供 sn 或 clientSn"
            ));
        }

        try {
            SqbResponse response = refundService.refund(sn, clientSn, refundAmount, operator, refundReason);

            Map<String, Object> result = new HashMap<>();
            String orderStatus = response.getOrderStatus();
            result.put("success", "REFUNDED".equals(orderStatus) || "PARTIAL_REFUNDED".equals(orderStatus));
            result.put("orderStatus", orderStatus);
            result.put("sn", response.getSn());
            result.put("clientSn", response.getClientSn());
            result.put("totalAmount", response.getTotalAmount());
            result.put("refundedAmount", response.getRefundedAmount());
            result.put("raw", response.getRawResponse().toString());
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "退款请求失败: " + e.getMessage()
            ));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "退款轮询被中断"
            ));
        }
    }
}
