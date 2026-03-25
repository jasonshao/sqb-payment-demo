package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.CancelCommand;
import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * 撤单操作接口
 */
public interface SqbCancelOperations {
    SqbResponse cancel(CancelCommand command, SqbRequestOptions options);
}
