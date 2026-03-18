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

    @Test
    void testSignDifferentBodyProducesDifferentSign() {
        String key = "same_key";
        String sign1 = SqbSignUtil.sign("{\"a\":1}", key);
        String sign2 = SqbSignUtil.sign("{\"a\":2}", key);
        assertNotEquals(sign1, sign2);
    }

    @Test
    void testSignDifferentKeyProducesDifferentSign() {
        String body = "{\"a\":1}";
        String sign1 = SqbSignUtil.sign(body, "key1");
        String sign2 = SqbSignUtil.sign(body, "key2");
        assertNotEquals(sign1, sign2);
    }

    @Test
    void testBuildAuthorizationFormat() {
        // 验证格式：{sn} {sign}，中间有且仅有一个空格
        String auth = SqbSignUtil.buildAuthorization("sn123", "{}", "key");
        String[] parts = auth.split(" ");
        assertEquals(2, parts.length, "Authorization 应由空格分隔为两部分");
        assertEquals("sn123", parts[0]);
        assertEquals(32, parts[1].length());
    }

    @Test
    void testSignWithChineseCharacters() {
        // 中文字符在 UTF-8 下的签名
        String body = "{\"subject\":\"星巴克咖啡\"}";
        String key = "testkey";
        String sign = SqbSignUtil.sign(body, key);
        assertNotNull(sign);
        assertEquals(32, sign.length());
    }

    @Test
    void testSignWithEmptyBody() {
        String sign = SqbSignUtil.sign("", "key");
        assertNotNull(sign);
        assertEquals(32, sign.length());
    }

    @Test
    void testVerifySignWithTamperedBody() {
        String body = "{\"amount\":\"100\"}";
        String key = "key";
        String sign = SqbSignUtil.sign(body, key);

        // 篡改 body 后验签应失败
        assertFalse(SqbSignUtil.verifySign("{\"amount\":\"999\"}", key, sign));
    }

    @Test
    void testVerifySignWithWrongKey() {
        String body = "{\"amount\":\"100\"}";
        String sign = SqbSignUtil.sign(body, "correct_key");

        assertFalse(SqbSignUtil.verifySign(body, "wrong_key", sign));
    }
}
