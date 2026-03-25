package com.example.sqbpayment.sdk;

import com.example.sqbpayment.domain.operations.*;
import com.example.sqbpayment.sdk.webhook.SqbWebhookOperations;

/**
 * 收钱吧 SDK 统一入口门面
 */
public interface SqbClient {
    SqbPaymentOperations payments();
    SqbRefundOperations refunds();
    SqbCancelOperations cancels();
    SqbQueryOperations queries();
    SqbTerminalOperations terminals();
    SqbWebhookOperations webhooks();
}
