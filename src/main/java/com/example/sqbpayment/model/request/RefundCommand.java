package com.example.sqbpayment.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 退款请求（Controller 入参）
 *
 * @param sn           收钱吧订单号（与 clientSn 二选一）
 * @param clientSn     商户订单号（与 sn 二选一）
 * @param refundAmount 退款金额，单位为分，必须为正整数
 * @param operator     操作员
 * @param refundReason 退款原因（可选）
 */
public record RefundCommand(
        String sn,
        String clientSn,
        @Positive(message = "退款金额必须为正整数") long refundAmount,
        @NotBlank(message = "操作员不能为空") String operator,
        String refundReason
) {
    /**
     * 校验：sn 和 clientSn 至少提供一个
     */
    public void validate() {
        if ((sn == null || sn.isBlank()) && (clientSn == null || clientSn.isBlank())) {
            throw new IllegalArgumentException("需要提供 sn 或 clientSn");
        }
    }
}
