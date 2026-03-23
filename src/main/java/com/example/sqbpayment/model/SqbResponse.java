package com.example.sqbpayment.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 收钱吧统一响应封装
 * 处理三层响应结构：通信层 -> 业务层 -> 数据层
 */
public class SqbResponse {

    private final JsonNode rawResponse;

    public SqbResponse(JsonNode rawResponse) {
        this.rawResponse = rawResponse;
    }

    /** 通信层 result_code */
    public String getResultCode() {
        return rawResponse.path("result_code").asText("");
    }

    /** 通信是否成功 */
    public boolean isCommunicationSuccess() {
        return "200".equals(getResultCode());
    }

    /** 通信层错误描述 */
    public String getErrorCode() {
        return rawResponse.path("error_code").asText("");
    }

    public String getErrorMessage() {
        return rawResponse.path("error_message").asText("");
    }

    /** 业务层 result_code */
    public String getBizResultCode() {
        return rawResponse.path("biz_response").path("result_code").asText("");
    }

    /** 业务层错误描述 */
    public String getBizErrorCode() {
        return rawResponse.path("biz_response").path("error_code").asText("");
    }

    public String getBizErrorMessage() {
        return rawResponse.path("biz_response").path("error_message").asText("");
    }

    /** 业务数据节点 */
    public JsonNode getData() {
        return rawResponse.path("biz_response").path("data");
    }

    /** 订单状态 */
    public String getOrderStatus() {
        return getData().path("order_status").asText(getData().path("status").asText(""));
    }

    /** 收钱吧订单号 */
    public String getSn() {
        return getData().path("sn").asText("");
    }

    /** 商户订单号 */
    public String getClientSn() {
        return getData().path("client_sn").asText("");
    }

    /** 交易金额（分） */
    public String getTotalAmount() {
        return getData().path("total_amount").asText("");
    }

    /** 实收金额（分） */
    public String getNetAmount() {
        return getData().path("net_amount").asText("");
    }

    /** 支付渠道交易号 */
    public String getTradeNo() {
        return getData().path("trade_no").asText("");
    }

    /** 完成时间 */
    public String getFinishTime() {
        return getData().path("finish_time").asText("");
    }

    /** terminal_sn（激活/签到返回） */
    public String getTerminalSn() {
        return getData().path("terminal_sn").asText("");
    }

    /** terminal_key（激活/签到返回） */
    public String getTerminalKey() {
        return getData().path("terminal_key").asText("");
    }

    /** 已退款金额（分） */
    public String getRefundedAmount() {
        return getData().path("refunded_amount").asText("");
    }

    /** 预创建二维码（precreate 返回） */
    public String getQrCode() {
        return getData().path("qr_code").asText("");
    }

    public JsonNode getRawResponse() {
        return rawResponse;
    }

    @Override
    public String toString() {
        return rawResponse.toString();
    }
}
