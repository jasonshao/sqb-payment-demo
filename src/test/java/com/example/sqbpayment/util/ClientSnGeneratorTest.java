package com.example.sqbpayment.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClientSnGeneratorTest {

    @Test
    void testGenerateUniqueness() {
        Set<String> sns = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            sns.add(ClientSnGenerator.generate());
        }
        assertEquals(1000, sns.size(), "生成的 client_sn 应全局唯一");
    }

    @Test
    void testGenerateFormat() {
        String sn = ClientSnGenerator.generate();
        // 14位时间戳 + 6位序列号 = 20位
        assertEquals(20, sn.length());
        assertTrue(sn.matches("\\d{20}"));
    }

    @Test
    void testGenerateRefundNo() {
        String refundNo = ClientSnGenerator.generateRefundNo();
        assertTrue(refundNo.startsWith("REF"));
        assertEquals(23, refundNo.length());
    }
}
