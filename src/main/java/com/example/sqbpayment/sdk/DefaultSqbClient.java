package com.example.sqbpayment.sdk;

import com.example.sqbpayment.domain.operations.*;
import com.example.sqbpayment.sdk.webhook.SqbWebhookOperations;
import org.springframework.stereotype.Component;

/**
 * SqbClient 默认实现，将各操作接口委托给 Spring 注入的 bean
 */
@Component
public class DefaultSqbClient implements SqbClient {

    private final SqbPaymentOperations paymentOperations;
    private final SqbRefundOperations refundOperations;
    private final SqbCancelOperations cancelOperations;
    private final SqbQueryOperations queryOperations;
    private final SqbTerminalOperations terminalOperations;
    private final SqbWebhookOperations webhookOperations;

    public DefaultSqbClient(SqbPaymentOperations paymentOperations,
                            SqbRefundOperations refundOperations,
                            SqbCancelOperations cancelOperations,
                            SqbQueryOperations queryOperations,
                            SqbTerminalOperations terminalOperations,
                            SqbWebhookOperations webhookOperations) {
        this.paymentOperations = paymentOperations;
        this.refundOperations = refundOperations;
        this.cancelOperations = cancelOperations;
        this.queryOperations = queryOperations;
        this.terminalOperations = terminalOperations;
        this.webhookOperations = webhookOperations;
    }

    @Override
    public SqbPaymentOperations payments() {
        return paymentOperations;
    }

    @Override
    public SqbRefundOperations refunds() {
        return refundOperations;
    }

    @Override
    public SqbCancelOperations cancels() {
        return cancelOperations;
    }

    @Override
    public SqbQueryOperations queries() {
        return queryOperations;
    }

    @Override
    public SqbTerminalOperations terminals() {
        return terminalOperations;
    }

    @Override
    public SqbWebhookOperations webhooks() {
        return webhookOperations;
    }
}
