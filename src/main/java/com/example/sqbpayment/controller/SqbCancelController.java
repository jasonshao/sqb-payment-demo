package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.request.CancelCommand;
import com.example.sqbpayment.model.response.OrderResult;
import com.example.sqbpayment.service.SqbCancelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * 撤单控制器
 */
@RestController
@RequestMapping("/api/cancel")
public class SqbCancelController {

    private final SqbCancelService cancelService;

    public SqbCancelController(SqbCancelService cancelService) {
        this.cancelService = cancelService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResult<OrderResult>>> cancel(
            @Valid @RequestBody CancelCommand command) throws IOException, InterruptedException {
        return cancelService.cancel(command)
                .thenApply(response -> {
                    OrderResult result = OrderResult.from(response);
                    boolean success = "PAY_CANCELED".equals(result.orderStatus())
                            || "CANCELED".equals(result.orderStatus());
                    return ResponseEntity.ok(new ApiResult<>(success, null, result));
                });
    }
}
