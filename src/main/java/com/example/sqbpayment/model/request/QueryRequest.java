package com.example.sqbpayment.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryRequest {

    @JsonProperty("terminal_sn")
    private String terminalSn;

    /** 收钱吧订单号（与 client_sn 二选一） */
    private String sn;

    /** 商户订单号（与 sn 二选一） */
    @JsonProperty("client_sn")
    private String clientSn;

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
}
