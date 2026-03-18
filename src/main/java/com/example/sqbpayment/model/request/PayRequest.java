package com.example.sqbpayment.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 付款码支付请求
 * 注意：total_amount 单位为分（1元 = 100分）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayRequest {

    @JsonProperty("terminal_sn")
    private String terminalSn;

    /** 商户订单号，必须全局唯一 */
    @JsonProperty("client_sn")
    private String clientSn;

    /** 交易金额，单位为分 */
    @JsonProperty("total_amount")
    private String totalAmount;

    /** 顾客付款码内容 */
    @JsonProperty("dynamic_id")
    private String dynamicId;

    /** 交易简介，显示在顾客账单中 */
    private String subject;

    /** 操作员 */
    private String operator;

    private String description;

    private String longitude;

    private String latitude;

    @JsonProperty("notify_url")
    private String notifyUrl;

    private String reflect;

    public String getTerminalSn() {
        return terminalSn;
    }

    public void setTerminalSn(String terminalSn) {
        this.terminalSn = terminalSn;
    }

    public String getClientSn() {
        return clientSn;
    }

    public void setClientSn(String clientSn) {
        this.clientSn = clientSn;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getDynamicId() {
        return dynamicId;
    }

    public void setDynamicId(String dynamicId) {
        this.dynamicId = dynamicId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public String getReflect() {
        return reflect;
    }

    public void setReflect(String reflect) {
        this.reflect = reflect;
    }
}
