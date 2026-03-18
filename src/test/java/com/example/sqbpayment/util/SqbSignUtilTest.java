package com.example.sqbpayment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqbSignUtilTest {

    @Test
    void testSign() {
        String body = "{\"terminal_sn\":\"test\"}";
        String key = "testkey";
        String sign = SqbSignUtil.sign(body, key);

        assertNotNull(sign);
        assertEquals(32, sign.length());
        // MD5 结果应为小写十六进制
        assertTrue(sign.matches("[0-9a-f]{32}"));
    }

    @Test
    void testBuildAuthorization() {
        String sn = "terminal001";
        String body = "{\"test\":\"value\"}";
        String key = "secret";

        String auth = SqbSignUtil.buildAuthorization(sn, body, key);

        assertTrue(auth.startsWith("terminal001 "));
        // sn + 空格 + 32位签名
        assertEquals("terminal001".length() + 1 + 32, auth.length());
    }

    @Test
    void testVerifySign() {
        String body = "{\"order\":\"123\"}";
        String key = "mykey";
        String sign = SqbSignUtil.sign(body, key);

        assertTrue(SqbSignUtil.verifySign(body, key, sign));
        assertFalse(SqbSignUtil.verifySign(body, key, "wrong_sign"));
    }

    @Test
    void testSignConsistency() {
        // 相同输入应产生相同签名
        String body = "{\"amount\":\"100\"}";
        String key = "key123";
        assertEquals(SqbSignUtil.sign(body, key), SqbSignUtil.sign(body, key));
    }
}
