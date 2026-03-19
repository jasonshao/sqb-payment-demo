package com.example.sqbpayment.controller;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.util.SqbSignUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SqbNotifyController.class)
class SqbNotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqbConfig config;

    private static final String TERMINAL_KEY = "test_terminal_key";
    private static final String TERMINAL_SN = "terminal001";

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.when(config.getTerminalKey()).thenReturn(TERMINAL_KEY);
    }

    // ========== 正常回调 ==========

    @Test
    void testNotifyWithValidSignature() throws Exception {
        String body = """
                {"sn":"789284025","client_sn":"order001","order_status":"PAID","status":"PAID","total_amount":"100"}
                """.trim();

        String sign = SqbSignUtil.sign(body, TERMINAL_KEY);
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
                {"sn":"789284025","client_sn":"order001","order_status":"REFUNDED","total_amount":"100","refunded_amount":"100"}
                """.trim();

        String sign = SqbSignUtil.sign(body, TERMINAL_KEY);
        String authorization = TERMINAL_SN + " " + sign;

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    // ========== 签名验证失败 ==========

    @Test
    void testNotifyWithInvalidSignature() throws Exception {
        String body = """
                {"sn":"789284025","client_sn":"order001","order_status":"PAID"}
                """.trim();

        String authorization = TERMINAL_SN + " " + "wrong_sign_value";

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().string("fail"));
    }

    @Test
    void testNotifyWithMissingAuthorization() throws Exception {
        String body = """
                {"sn":"789284025","order_status":"PAID"}
                """.trim();

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("fail"));
    }

    @Test
    void testNotifyWithMalformedAuthorization() throws Exception {
        String body = """
                {"sn":"789284025","order_status":"PAID"}
                """.trim();

        // Authorization 头缺少空格分隔符
        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "no_space_here"))
                .andExpect(status().isOk())
                .andExpect(content().string("fail"));
    }

    // ========== 不同订单状态的回调 ==========

    @Test
    void testNotifyPayCanceled() throws Exception {
        String body = """
                {"sn":"789284025","client_sn":"order001","order_status":"PAY_CANCELED"}
                """.trim();

        String sign = SqbSignUtil.sign(body, TERMINAL_KEY);

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
                {"sn":"789284025","client_sn":"order001","order_status":"PARTIAL_REFUNDED","refunded_amount":"50"}
                """.trim();

        String sign = SqbSignUtil.sign(body, TERMINAL_KEY);

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TERMINAL_SN + " " + sign))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }

    // ========== 回调报文解析 ==========

    @Test
    void testNotifyWithFallbackToStatusField() throws Exception {
        // order_status 不存在时，应回退到 status 字段
        String body = """
                {"sn":"789284025","client_sn":"order001","status":"PAID"}
                """.trim();

        String sign = SqbSignUtil.sign(body, TERMINAL_KEY);

        mockMvc.perform(post("/api/notify")
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", TERMINAL_SN + " " + sign))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));
    }
}
