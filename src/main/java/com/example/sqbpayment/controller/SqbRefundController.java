package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.request.RefundCommand;
import com.example.sqbpayment.model.response.OrderResult;
import com.example.sqbpayment.service.SqbRefundService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResult<OrderResult>>> refund(
            @Valid @RequestBody RefundCommand command) throws IOException, InterruptedException {
        return refundService.refund(command)
                .thenApply(response -> {
                    OrderResult result = OrderResult.from(response);
                    boolean success = "REFUNDED".equals(result.orderStatus())
                            || "PARTIAL_REFUNDED".equals(result.orderStatus());
                    return ResponseEntity.ok(new ApiResult<>(success, null, result));
                });
    }
}
