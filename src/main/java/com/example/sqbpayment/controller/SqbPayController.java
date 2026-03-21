package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.request.PayCommand;
import com.example.sqbpayment.model.response.OrderResult;
import com.example.sqbpayment.service.SqbPayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResult<OrderResult>>> pay(
            @Valid @RequestBody PayCommand command) throws IOException, InterruptedException {
        return payService.pay(command)
                .thenApply(response -> {
                    OrderResult result = OrderResult.from(response);
                    boolean success = "PAID".equals(result.orderStatus());
                    return ResponseEntity.ok(new ApiResult<>(success, null, result));
                });
    }
}
