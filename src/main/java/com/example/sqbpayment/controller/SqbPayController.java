package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 付款码支付控制器（B扫C）
 */
@RestController
@RequestMapping("/api/pay")
public class SqbPayController {

    private final SqbPayService payService;

    public SqbPayController(SqbPayService payService) {
        this.payService = payService;
    }

    /**
     * 发起付款码支付
     *
     * 请求参数：
     * - dynamicId: 顾客付款码内容（必填）
     * - totalAmount: 金额，单位为分（必填）
     * - subject: 交易简介（必填）
     * - operator: 操作员（必填）
     * - notifyUrl: 回调地址（选填）
     *
     * 该接口会自动处理轮询逻辑，返回最终支付结果
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<Map<String, Object>>> pay(@RequestBody Map<String, String> params) {
        String dynamicId = params.get("dynamicId");
        String totalAmount = params.get("totalAmount");
        String subject = params.get("subject");
        String operator = params.get("operator");
        String notifyUrl = params.get("notifyUrl");

        if (dynamicId == null || totalAmount == null || subject == null || operator == null) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "缺少必填参数：dynamicId, totalAmount, subject, operator"
            )));
        }

        try {
            return payService.pay(dynamicId, totalAmount, subject, operator, notifyUrl)
                    .thenApply(response -> ResponseEntity.ok(buildPayResult(response)));
        } catch (IOException e) {
            return CompletableFuture.completedFuture(ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "支付请求失败: " + e.getMessage()
            )));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "支付轮询被中断"
            )));
        }
    }

    private Map<String, Object> buildPayResult(SqbResponse response) {
        Map<String, Object> result = new HashMap<>();
        String orderStatus = response.getOrderStatus();
        result.put("success", "PAID".equals(orderStatus));
        result.put("orderStatus", orderStatus);
        result.put("sn", response.getSn());
        result.put("clientSn", response.getClientSn());
        result.put("totalAmount", response.getTotalAmount());
        result.put("netAmount", response.getNetAmount());
        result.put("tradeNo", response.getTradeNo());
        result.put("finishTime", response.getFinishTime());
        result.put("raw", response.getRawResponse().toString());
        return result;
    }
}
