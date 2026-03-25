package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * 终端管理操作接口（激活 + 签到）
 */
public interface SqbTerminalOperations {
    SqbResponse activate(String code, String deviceId, String name, SqbRequestOptions options);
    SqbResponse checkin(SqbRequestOptions options);
}
