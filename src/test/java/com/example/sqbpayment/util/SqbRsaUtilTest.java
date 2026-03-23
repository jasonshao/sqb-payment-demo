package com.example.sqbpayment.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class SqbRsaUtilTest {

    private static KeyPair keyPair;
    private static String publicKeyBase64;

    @BeforeAll
    static void initKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    private String signData(String data) throws Exception {
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    @Test
    void testVerifyValidSignature() throws Exception {
        String data = "{\"order_status\":\"PAID\",\"sn\":\"123456\"}";
        String signatureBase64 = signData(data);

        assertTrue(SqbRsaUtil.verifySign(data, publicKeyBase64, signatureBase64));
    }

    @Test
    void testVerifyInvalidSignature() {
        String data = "{\"order_status\":\"PAID\"}";
        assertFalse(SqbRsaUtil.verifySign(data, publicKeyBase64, "invalidBase64Signature=="));
    }

    @Test
    void testVerifyTamperedData() throws Exception {
        String data = "{\"order_status\":\"PAID\"}";
        String signatureBase64 = signData(data);

        String tampered = "{\"order_status\":\"REFUNDED\"}";
        assertFalse(SqbRsaUtil.verifySign(tampered, publicKeyBase64, signatureBase64));
    }

    @Test
    void testVerifyWithInvalidPublicKey() {
        String data = "test data";
        assertFalse(SqbRsaUtil.verifySign(data, "not_a_valid_key", "not_a_valid_sig"));
    }

    @Test
    void testLoadPublicKey() throws Exception {
        PublicKey key = SqbRsaUtil.loadPublicKey(publicKeyBase64);
        assertNotNull(key);
        assertEquals("RSA", key.getAlgorithm());
    }
}
