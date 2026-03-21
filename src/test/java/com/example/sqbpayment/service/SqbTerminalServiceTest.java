package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.ActivateRequest;
import com.example.sqbpayment.model.request.CheckinRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private SqbApiTemplate apiTemplate;

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

        service = new SqbTerminalService(config, apiTemplate);
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
        when(apiTemplate.callAsVendor(eq("/terminal/activate"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = service.activate("code123", "device001", "收银台1号");

        assertTrue(result.isCommunicationSuccess());
        assertEquals("ACTIVATE_SUCCESS", result.getBizResultCode());
        assertEquals("new_terminal_sn", config.getTerminalSn());
        assertEquals("new_terminal_key", config.getTerminalKey());
    }

    @Test
    void testActivateUsesVendorLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"ACTIVATE_SUCCESS","data":{"terminal_sn":"t1","terminal_key":"k1"}}}
                """;
        when(apiTemplate.callAsVendor(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        service.activate("code", "device", null);

        // 验证使用了 callAsVendor（非 call）
        verify(apiTemplate).callAsVendor(eq("/terminal/activate"), any());
        verify(apiTemplate, never()).call(anyString(), any());
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
        when(apiTemplate.callAsVendor(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = service.activate("bad_code", "device", null);

        assertEquals("ACTIVATE_FAIL", result.getBizResultCode());
        assertEquals("terminal001", config.getTerminalSn());
        assertEquals("terminalkey001", config.getTerminalKey());
    }

    @Test
    void testActivateCommunicationError() throws Exception {
        String responseJson = """
                {"result_code":"500","error_message":"服务器内部错误"}
                """;
        when(apiTemplate.callAsVendor(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = service.activate("code", "device", null);

        assertFalse(result.isCommunicationSuccess());
        assertEquals("terminal001", config.getTerminalSn());
    }

    @Test
    void testActivateNetworkException() throws Exception {
        when(apiTemplate.callAsVendor(anyString(), any()))
                .thenThrow(new IOException("网络超时"));

        assertThrows(IOException.class, () -> service.activate("code", "device", null));
    }

    @Test
    void testActivateRequestContainsAppId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"ACTIVATE_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(apiTemplate.callAsVendor(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        service.activate("code", "device001", "terminal_name");

        ArgumentCaptor<ActivateRequest> captor = ArgumentCaptor.forClass(ActivateRequest.class);
        verify(apiTemplate).callAsVendor(anyString(), captor.capture());
        assertEquals("app001", captor.getValue().getAppId());
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
        when(apiTemplate.call(eq("/terminal/checkin"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = service.checkin();

        assertTrue(result.isCommunicationSuccess());
        assertEquals("TERMINAL_CHECKIN_SUCCESS", result.getBizResultCode());
        assertEquals("rotated_key_new", config.getTerminalKey());
    }

    @Test
    void testCheckinUsesTerminalLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        service.checkin();

        // 验证使用了 call（非 callAsVendor）
        verify(apiTemplate).call(eq("/terminal/checkin"), any());
        verify(apiTemplate, never()).callAsVendor(anyString(), any());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = service.checkin();

        assertEquals("TERMINAL_CHECKIN_FAIL", result.getBizResultCode());
        assertEquals("terminalkey001", config.getTerminalKey());
    }

    @Test
    void testCheckinRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        service.checkin();

        ArgumentCaptor<CheckinRequest> captor = ArgumentCaptor.forClass(CheckinRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }

    @Test
    void testCheckinRequestContainsDeviceId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"TERMINAL_CHECKIN_SUCCESS","data":{"terminal_sn":"t","terminal_key":"k"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        service.checkin();

        ArgumentCaptor<CheckinRequest> captor = ArgumentCaptor.forClass(CheckinRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("device001", captor.getValue().getDeviceId());
    }
}
