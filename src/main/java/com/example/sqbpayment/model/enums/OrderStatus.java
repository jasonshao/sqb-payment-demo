package com.example.sqbpayment.model.enums;

import java.util.Set;

/**
 * 收钱吧订单状态枚举
 */
public enum OrderStatus {

    CREATED("订单已创建", false),
    PAID("支付成功", true),
    PAY_CANCELED("支付失败/已撤销", true),
    PAY_ERROR("支付异常", false),
    REFUNDED("全额退款", true),
    PARTIAL_REFUNDED("部分退款", true),
    REFUND_ERROR("退款异常", false),
    CANCELED("已撤销", true),
    CANCEL_ERROR("撤销异常", false);

    private static final Set<OrderStatus> FINAL_STATES = Set.of(
            PAID, PAY_CANCELED, REFUNDED, PARTIAL_REFUNDED, CANCELED
    );

    private final String description;
    private final boolean finalState;

    OrderStatus(String description, boolean finalState) {
        this.description = description;
        this.finalState = finalState;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public static boolean isFinal(String status) {
        try {
            return valueOf(status).isFinalState();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
