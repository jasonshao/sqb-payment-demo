package com.example.sqbpayment.util;

/**
 * @deprecated 已迁移到 {@link com.example.sqbpayment.sdk.signing.SqbSignUtil}
 */
@Deprecated
public final class SqbSignUtil {

    private SqbSignUtil() {
    }

    public static String sign(String requestBody, String key) {
        return com.example.sqbpayment.sdk.signing.SqbSignUtil.sign(requestBody, key);
    }

    public static String buildAuthorization(String sn, String requestBody, String key) {
        return com.example.sqbpayment.sdk.signing.SqbSignUtil.buildAuthorization(sn, requestBody, key);
    }

    public static boolean verifySign(String requestBody, String key, String receivedSign) {
        return com.example.sqbpayment.sdk.signing.SqbSignUtil.verifySign(requestBody, key, receivedSign);
    }
}
