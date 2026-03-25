package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.RefundCommand;
import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * 退款操作接口
 */
public interface SqbRefundOperations {
    SqbResponse refund(RefundCommand command, SqbRequestOptions options);
}
