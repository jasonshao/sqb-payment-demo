package com.example.sqbpayment.util;

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

    /**
     * 计算签名：MD5(requestBody + key)
     *
     * @param requestBody JSON 请求体原始字符串
     * @param key         vendor_key 或 terminal_key
     * @return 32 位小写 MD5 签名
     */
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

    /**
     * 构建 Authorization 请求头
     * 格式：{sn} {sign}（中间一个空格）
     *
     * @param sn          vendor_sn 或 terminal_sn
     * @param requestBody JSON 请求体原始字符串
     * @param key         vendor_key 或 terminal_key
     * @return Authorization 头的值
     */
    public static String buildAuthorization(String sn, String requestBody, String key) {
        return sn + " " + sign(requestBody, key);
    }

    /**
     * 验证签名（用于回调验签）
     */
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
