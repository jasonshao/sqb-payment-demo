package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.request.PrecreateCommand;
import com.example.sqbpayment.model.response.OrderResult;
import com.example.sqbpayment.service.SqbPrecreateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 预创建支付控制器（C2B / 客扫商户码）
 */
@RestController
@RequestMapping("/api/precreate")
public class SqbPrecreateController {

    private final SqbPrecreateService precreateService;

    public SqbPrecreateController(SqbPrecreateService precreateService) {
        this.precreateService = precreateService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResult<OrderResult>>> precreate(
            @Valid @RequestBody PrecreateCommand command) throws IOException, InterruptedException {
        return precreateService.precreate(command)
                .thenApply(response -> {
                    OrderResult result = OrderResult.from(response);
                    boolean success = result.qrCode() != null || "PAID".equals(result.orderStatus());
                    return ResponseEntity.ok(new ApiResult<>(success, null, result));
                });
    }
}
