package com.example.sqbpayment.sdk.signing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 收钱吧 RSA 验签工具类
 * 用于异步回调通知的签名验证（SHA256WithRSA）
 */
public final class SqbRsaUtil {

    private static final Logger log = LoggerFactory.getLogger(SqbRsaUtil.class);

    private SqbRsaUtil() {
    }

    public static boolean verifySign(String data, String publicKeyBase64, String signatureBase64) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            log.warn("RSA签名验证异常", e);
            return false;
        }
    }

    public static PublicKey loadPublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
