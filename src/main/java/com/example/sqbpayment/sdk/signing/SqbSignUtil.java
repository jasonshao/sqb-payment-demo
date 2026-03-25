package com.example.sqbpayment.sdk.signing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 收钱吧签名工具类
 * 签名算法：MD5(request_body + key)
 */
public final class SqbSignUtil {

    private SqbSignUtil() {
    }

    public static String sign(String requestBody, String key) {
        String raw = requestBody + key;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    public static String buildAuthorization(String sn, String requestBody, String key) {
        return sn + " " + sign(requestBody, key);
    }

    public static boolean verifySign(String requestBody, String key, String receivedSign) {
        String expectedSign = sign(requestBody, key);
        return MessageDigest.isEqual(
                expectedSign.getBytes(StandardCharsets.UTF_8),
                receivedSign.getBytes(StandardCharsets.UTF_8));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
