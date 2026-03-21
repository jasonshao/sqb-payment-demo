package com.example.sqbpayment.model.response;

import com.example.sqbpayment.model.SqbResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 订单类响应（支付、查询、退款共用）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResult(
        String orderStatus,
        String sn,
        String clientSn,
        String totalAmount,
        String netAmount,
        String refundedAmount,
        String tradeNo,
        String finishTime
) {
    public static OrderResult from(SqbResponse r) {
        return new OrderResult(
                emptyToNull(r.getOrderStatus()),
                emptyToNull(r.getSn()),
                emptyToNull(r.getClientSn()),
                emptyToNull(r.getTotalAmount()),
                emptyToNull(r.getNetAmount()),
                emptyToNull(r.getRefundedAmount()),
                emptyToNull(r.getTradeNo()),
                emptyToNull(r.getFinishTime())
        );
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
