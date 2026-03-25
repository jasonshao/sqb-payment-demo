package com.example.sqbpayment.util;

import java.security.PublicKey;

/**
 * @deprecated 已迁移到 {@link com.example.sqbpayment.sdk.signing.SqbRsaUtil}
 */
@Deprecated
public final class SqbRsaUtil {

    private SqbRsaUtil() {
    }

    public static boolean verifySign(String data, String publicKeyBase64, String signatureBase64) {
        return com.example.sqbpayment.sdk.signing.SqbRsaUtil.verifySign(data, publicKeyBase64, signatureBase64);
    }

    public static PublicKey loadPublicKey(String base64) throws Exception {
        return com.example.sqbpayment.sdk.signing.SqbRsaUtil.loadPublicKey(base64);
    }
}
