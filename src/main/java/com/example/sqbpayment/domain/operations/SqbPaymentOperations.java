package com.example.sqbpayment.domain.operations;

import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.PayCommand;
import com.example.sqbpayment.model.request.PrecreateCommand;
import com.example.sqbpayment.sdk.SqbRequestOptions;

/**
 * 支付操作接口（付款码支付 + 预创建）
 */
public interface SqbPaymentOperations {
    SqbResponse pay(PayCommand command, SqbRequestOptions options);
    SqbResponse precreate(PrecreateCommand command, SqbRequestOptions options);
}
