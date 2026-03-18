package com.example.sqbpayment.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    void testFinalStates() {
        assertTrue(OrderStatus.PAID.isFinalState());
        assertTrue(OrderStatus.PAY_CANCELED.isFinalState());
        assertTrue(OrderStatus.REFUNDED.isFinalState());
        assertTrue(OrderStatus.PARTIAL_REFUNDED.isFinalState());
        assertTrue(OrderStatus.CANCELED.isFinalState());
    }

    @Test
    void testNonFinalStates() {
        assertFalse(OrderStatus.CREATED.isFinalState());
        assertFalse(OrderStatus.PAY_ERROR.isFinalState());
        assertFalse(OrderStatus.REFUND_ERROR.isFinalState());
        assertFalse(OrderStatus.CANCEL_ERROR.isFinalState());
    }

    @Test
    void testIsFinalWithString() {
        assertTrue(OrderStatus.isFinal("PAID"));
        assertTrue(OrderStatus.isFinal("PAY_CANCELED"));
        assertTrue(OrderStatus.isFinal("REFUNDED"));
        assertTrue(OrderStatus.isFinal("PARTIAL_REFUNDED"));
        assertTrue(OrderStatus.isFinal("CANCELED"));
    }

    @Test
    void testIsFinalWithNonFinalString() {
        assertFalse(OrderStatus.isFinal("CREATED"));
        assertFalse(OrderStatus.isFinal("PAY_ERROR"));
        assertFalse(OrderStatus.isFinal("REFUND_ERROR"));
        assertFalse(OrderStatus.isFinal("CANCEL_ERROR"));
    }

    @Test
    void testIsFinalWithUnknownStatus() {
        assertFalse(OrderStatus.isFinal("UNKNOWN"));
        assertFalse(OrderStatus.isFinal(""));
        assertFalse(OrderStatus.isFinal("null"));
    }

    @Test
    void testDescriptions() {
        assertEquals("支付成功", OrderStatus.PAID.getDescription());
        assertEquals("支付失败/已撤销", OrderStatus.PAY_CANCELED.getDescription());
        assertEquals("全额退款", OrderStatus.REFUNDED.getDescription());
        assertEquals("部分退款", OrderStatus.PARTIAL_REFUNDED.getDescription());
        assertEquals("订单已创建", OrderStatus.CREATED.getDescription());
    }

    @Test
    void testAllEnumValuesHaveDescription() {
        for (OrderStatus status : OrderStatus.values()) {
            assertNotNull(status.getDescription());
            assertFalse(status.getDescription().isEmpty());
        }
    }
}
