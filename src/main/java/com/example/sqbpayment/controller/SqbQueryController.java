package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.service.SqbQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单查询控制器
 */
@RestController
@RequestMapping("/api/query")
public class SqbQueryController {

    private final SqbQueryService queryService;

    public SqbQueryController(SqbQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 查询订单
     * 通过 sn（收钱吧订单号）或 clientSn（商户订单号）查询，二选一
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> query(@RequestBody Map<String, String> params) {
        String sn = params.get("sn");
        String clientSn = params.get("clientSn");

        if (sn == null && clientSn == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "需要提供 sn 或 clientSn"
            ));
        }

        try {
            SqbResponse response;
            if (sn != null && !sn.isEmpty()) {
                response = queryService.queryBySn(sn);
            } else {
                response = queryService.queryByClientSn(clientSn);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isCommunicationSuccess());
            result.put("orderStatus", response.getOrderStatus());
            result.put("sn", response.getSn());
            result.put("clientSn", response.getClientSn());
            result.put("totalAmount", response.getTotalAmount());
            result.put("netAmount", response.getNetAmount());
            result.put("refundedAmount", response.getRefundedAmount());
            result.put("raw", response.getRawResponse().toString());
            return ResponseEntity.ok(result);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "查询请求失败: " + e.getMessage()
            ));
        }
    }
}
