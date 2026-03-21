package com.example.sqbpayment.model.response;

import com.example.sqbpayment.model.SqbResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 终端管理类响应（激活、签到共用）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TerminalResult(
        String resultCode,
        String bizResultCode,
        String terminalSn,
        String terminalKey
) {
    public static TerminalResult from(SqbResponse r) {
        return new TerminalResult(
                r.getResultCode(),
                r.getBizResultCode(),
                emptyToNull(r.getTerminalSn()),
                emptyToNull(r.getTerminalKey())
        );
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
