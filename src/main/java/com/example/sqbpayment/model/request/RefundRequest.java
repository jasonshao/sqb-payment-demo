package com.example.sqbpayment.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 退款请求
 * 注意：refund_amount 单位为分
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundRequest {

    @JsonProperty("terminal_sn")
    private String terminalSn;

    /** 收钱吧订单号（与 client_sn 二选一） */
    private String sn;

    /** 商户订单号（与 sn 二选一） */
    @JsonProperty("client_sn")
    private String clientSn;

    /** 退款请求号，必须唯一 */
    @JsonProperty("refund_request_no")
    private String refundRequestNo;

    /** 退款金额，单位为分 */
    @JsonProperty("refund_amount")
    private String refundAmount;

    /** 操作员 */
    private String operator;

    @JsonProperty("refund_reason")
    private String refundReason;

    public String getTerminalSn() {
        return terminalSn;
    }

    public void setTerminalSn(String terminalSn) {
        this.terminalSn = terminalSn;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getClientSn() {
        return clientSn;
    }

    public void setClientSn(String clientSn) {
        this.clientSn = clientSn;
    }

    public String getRefundRequestNo() {
        return refundRequestNo;
    }

    public void setRefundRequestNo(String refundRequestNo) {
        this.refundRequestNo = refundRequestNo;
    }

    public String getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(String refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }
}
