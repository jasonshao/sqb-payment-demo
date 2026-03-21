package com.example.sqbpayment.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 付款码支付请求（Controller 入参）
 *
 * @param dynamicId   顾客付款码内容（扫码枪扫描获得）
 * @param totalAmount 金额，单位为分，必须为正整数
 * @param subject     交易简介，显示在顾客账单中
 * @param operator    操作员
 * @param notifyUrl   异步回调通知地址（可选）
 */
public record PayCommand(
        @NotBlank(message = "付款码不能为空") String dynamicId,
        @Positive(message = "金额必须为正整数") long totalAmount,
        @NotBlank(message = "交易简介不能为空") String subject,
        @NotBlank(message = "操作员不能为空") String operator,
        String notifyUrl
) {
}
