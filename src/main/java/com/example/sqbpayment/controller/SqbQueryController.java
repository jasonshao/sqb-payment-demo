package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.model.response.OrderResult;
import com.example.sqbpayment.service.SqbQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    public ResponseEntity<ApiResult<OrderResult>> query(@RequestBody Map<String, String> params) {
        String sn = params.get("sn");
        String clientSn = params.get("clientSn");

        if ((sn == null || sn.isBlank()) && (clientSn == null || clientSn.isBlank())) {
            throw new IllegalArgumentException("需要提供 sn 或 clientSn");
        }

        var response = (sn != null && !sn.isBlank())
                ? queryService.queryBySn(sn)
                : queryService.queryByClientSn(clientSn);

        return ResponseEntity.ok(new ApiResult<>(
                response.isCommunicationSuccess(), null, OrderResult.from(response)));
    }
}
