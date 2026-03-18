package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.util.SqbHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqbTerminalServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbHttpClient httpClient;

    private SqbConfig config;
    private SqbTerminalService service;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setVendorSn("vendor001");
        config.setVendorKey("vendorkey001");
        config.setAppId("app001");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");
        config.setDeviceId("device001");

        when(httpClient.getObjectMapper()).thenReturn(new ObjectMapper());

        service = new SqbTerminalService(config, httpClient);
    }

    // ========== 激活测试 ==========

    @Test
    void testActivateSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"ACTIVATE_SUCCESS",
                        "data":{
                            "terminal_sn":"new_terminal_sn",
                            "terminal_key":"new_terminal_key",
                            "terminal_name":"收银台1号"
                        }
                    }
                }
                """;
        when(httpClient.execute(
                eq("https://vsi-api.shouqianba.com/terminal/activate"),
                anyString(),
                eq("vendor001"),
                eq("vendorkey001")
        )).thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = service.activate("code123", "device001", "收银台1号");

        assertTrue(result.isCommunicationSuccess());
        assertEquals("ACTIVATE_SUCCESS", result.getBizResultCode());
        // 验证 terminal_sn 和 terminal_key 已更新到配置
        assertEquals("new_terminal_sn", config.getTerminalSn());
        assertEquals("new_terminal_key", config.getTerminalKey());
    }

    @Test
    void testActivateUsesVendorLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"ACTIVATE_SUCCESS","data":{"terminal_sn":"t1","terminal_key":"k1"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        service.activate("code", "device", null);

        // 验证使用了 vendor_sn 和 vendor_key（非 terminal 级别）
        verify(httpClient).execute(
                contains("/terminal/activate"),
                anyString(),
                eq("vendor001"),
                eq("vendorkey001")
        );
    }

    @Test
    void testActivateFailure() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"ACTIVATE_FAIL",
                        "error_code":"INVALID_CODE",
                        "error_message":"激活码无效"
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = service.activate("bad_code", "device", null);

        assertEquals("ACTIVATE_FAIL", result.getBizResultCode());
        // 配置不应被更新
        assertEquals("terminal001", config.getTerminalSn());
        assertEquals("terminalkey001", config.getTerminalKey());
    }

    @Test
    void testActivateCommunicationError() throws Exception {
        String responseJson = """
                {"result_code":"500","error_message":"服务器内部错误"}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = service.activate("code", "device", null);

        assertFalse(result.isCommunicationSuccess());
        assertEquals("terminal001", config.getTerminalSn()); // 未更新
    }

    @Test
    void testActivateNetworkException() throws Exception {
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IOException("网络超时"));

        assertThrows(IOException.class, () -> service.activate("code", "device", null));
    }

    @Test
    void testActivateRequestContainsAppId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"ACTIVATE_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        service.activate("code", "device001", "terminal_name");

        // 验证请求体包含 app_id
        verify(httpClient).execute(anyString(), contains("app001"), anyString(), anyString());
    }

    // ========== 签到测试 ==========

    @Test
    void testCheckinSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"TERMINAL_CHECKIN_SUCCESS",
                        "data":{
                            "terminal_sn":"terminal001",
                            "terminal_key":"rotated_key_new"
                        }
                    }
                }
                """;
        when(httpClient.execute(
                eq("https://vsi-api.shouqianba.com/terminal/checkin"),
                anyString(),
                eq("terminal001"),
                eq("terminalkey001")
        )).thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = service.checkin();

        assertTrue(result.isCommunicationSuccess());
        assertEquals("TERMINAL_CHECKIN_SUCCESS", result.getBizResultCode());
        // 关键：验证 terminal_key 已被轮换更新
        assertEquals("rotated_key_new", config.getTerminalKey());
    }

    @Test
    void testCheckinUsesTerminalLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        service.checkin();

        // 验证使用了 terminal_sn 和 terminal_key（非 vendor 级别）
        verify(httpClient).execute(
                contains("/terminal/checkin"),
                anyString(),
                eq("terminal001"),
                eq("terminalkey001")
        );
    }

    @Test
    void testCheckinFailureKeyNotUpdated() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"TERMINAL_CHECKIN_FAIL",
                        "error_code":"ILLEGAL_SIGN",
                        "error_message":"签名错误"
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = service.checkin();

        assertEquals("TERMINAL_CHECKIN_FAIL", result.getBizResultCode());
        // 签到失败时 terminal_key 不应更新
        assertEquals("terminalkey001", config.getTerminalKey());
    }

    @Test
    void testCheckinRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        service.checkin();

        verify(httpClient).execute(anyString(), contains("terminal001"), anyString(), anyString());
    }

    @Test
    void testCheckinRequestContainsDeviceId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        service.checkin();

        verify(httpClient).execute(anyString(), contains("device001"), anyString(), anyString());
    }
}
