package com.example.sqbpayment.util;

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

    private SqbRsaUtil() {
    }

    /**
     * RSA SHA256WithRSA 签名验证
     *
     * @param data            原始数据（请求体）
     * @param publicKeyBase64 RSA 公钥 Base64 字符串
     * @param signatureBase64 签名 Base64 字符串
     * @return 验签是否通过
     */
    public static boolean verifySign(String data, String publicKeyBase64, String signatureBase64) {
        try {
            PublicKey publicKey = loadPublicKey(publicKeyBase64);
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureBase64));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 Base64 字符串加载 RSA 公钥
     */
    public static PublicKey loadPublicKey(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
