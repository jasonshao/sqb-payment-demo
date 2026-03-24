package com.example.sqbpayment.controller;

import com.example.sqbpayment.config.SqbConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SqbNotifyController.class)
class SqbNotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqbConfig config;

    private static KeyPair keyPair;
    private static String publicKeyBase64;
    private static final String TERMINAL_SN = "terminal001";

    @BeforeAll
    static void initKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(config.getNotifyPublicKey()).thenReturn(publicKeyBase64);
    }

    private String rsaSign(String data) throws Exception {
        Signature signature = Signature.getInstance("SHA256WithRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    // ========== 正常回调 ==========

    @Test
    void testNotifyWithValidSignature() throws Exception {
        String body = """
                {"sn":"789284025","client_sn":"order001","order_status":"PAID","status":"PAID","total_amount":"100"}
                """.trim();

        String sign = rsaSign(body);
        String authorization = TERMINAL_SN + " " + sign;

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    void testNotifyWithRefundedStatus() throws Exception {
        String body = """
                {"sn":"789284026","client_sn":"order002","order_status":"REFUNDED","total_amount":"100","refunded_amount":"100"}
                """.trim();

        String sign = rsaSign(body);
        String authorization = TERMINAL_SN + " " + sign;

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    // ========== 签名验证失败 → HTTP 403 ==========

    @Test
    void testNotifyWithInvalidSignatureReturns403() throws Exception {
        String body = """
                {"sn":"789284025","client_sn":"order001","order_status":"PAID"}
                """.trim();

        String authorization = TERMINAL_SN + " " + "not_a_valid_base64_signature";

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isForbidden())
                .andExpect(content().string("signature verification failed"));
    }

    @Test
    void testNotifyWithMissingAuthorizationReturns403() throws Exception {
        String body = """
                {"sn":"789284025","order_status":"PAID"}
                """.trim();

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("signature verification failed"));
    }

    @Test
    void testNotifyWithMalformedAuthorizationReturns403() throws Exception {
        String body = """
                {"sn":"789284025","order_status":"PAID"}
                """.trim();

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "no_space_here"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("signature verification failed"));
    }

    // ========== 不同订单状态的回调 ==========

    @Test
    void testNotifyPayCanceled() throws Exception {
        String body = """
                {"sn":"789284027","client_sn":"order003","order_status":"PAY_CANCELED"}
                """.trim();

        String sign = rsaSign(body);

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TERMINAL_SN + " " + sign))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    void testNotifyPartialRefunded() throws Exception {
        String body = """
                {"sn":"789284028","client_sn":"order004","order_status":"PARTIAL_REFUNDED","refunded_amount":"50"}
                """.trim();

        String sign = rsaSign(body);

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TERMINAL_SN + " " + sign))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    // ========== 回调报文解析 ==========

    @Test
    void testDuplicateNotificationIsIdempotent() throws Exception {
        String body = """
                {"sn":"DUP001","client_sn":"orderDup","order_status":"PAID","status":"PAID","total_amount":"100"}
                """.trim();

        String sign = rsaSign(body);
        String authorization = TERMINAL_SN + " " + sign;

        // 第一次调用
        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        // 第二次调用（重复通知）应幂等返回 success
        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    @Test
    void testNotifyWithFallbackToStatusField() throws Exception {
        String body = """
                {"sn":"789284029","client_sn":"order005","status":"PAID"}
                """.trim();

        String sign = rsaSign(body);

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TERMINAL_SN + " " + sign))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }
}
