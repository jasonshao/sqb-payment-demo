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
class SqbPayServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbHttpClient httpClient;

    @Mock
    private SqbQueryService queryService;

    private SqbConfig config;
    private SqbPayService payService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        when(httpClient.getObjectMapper()).thenReturn(new ObjectMapper());

        payService = new SqbPayService(config, httpClient, queryService);
    }

    // ========== 支付成功场景 ==========

    @Test
    void testPaySuccessImmediate() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"20230615001",
                            "order_status":"PAID",
                            "total_amount":"100",
                            "net_amount":"97"
                        }
                    }
                }
                """;
        when(httpClient.execute(contains("/upay/v2/pay"), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = payService.pay("130818341921600584", "100", "测试商品", "cashier01", null);

        assertEquals("PAID", result.getOrderStatus());
        assertEquals("100", result.getTotalAmount());
        // 不应触发轮询
        verifyNoInteractions(queryService);
    }

    // ========== 支付失败场景 ==========

    @Test
    void testPayFailImmediate() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_FAIL",
                        "error_code":"INVALID_BARCODE",
                        "error_message":"无效的付款码"
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = payService.pay("invalid_code", "100", "测试", "op", null);

        assertEquals("PAY_FAIL", result.getBizResultCode());
        verifyNoInteractions(queryService);
    }

    // ========== 支付中 -> 轮询场景 ==========

    @Test
    void testPayInProgressTriggersPolling() throws Exception {
        String payResponseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_IN_PROGRESS",
                        "data":{"order_status":"CREATED","client_sn":"sn001"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(payResponseJson));

        // 模拟轮询结果
        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID","client_sn":"sn001"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(pollResult);

        SqbResponse result = payService.pay("code", "100", "test", "op", null);

        assertEquals("PAID", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }

    @Test
    void testPayFailErrorTriggersPolling() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_FAIL_ERROR",
                        "data":{"order_status":"PAY_ERROR","client_sn":"sn002"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAY_CANCELED"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(pollResult);

        SqbResponse result = payService.pay("code", "100", "test", "op", null);

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }

    // ========== 通信失败场景 ==========

    @Test
    void testPayCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"400","error_code":"ILLEGAL_SIGN","error_message":"签名错误"}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = payService.pay("code", "100", "test", "op", null);

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testPayNetworkException() throws Exception {
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IOException("连接超时"));

        assertThrows(IOException.class,
                () -> payService.pay("code", "100", "test", "op", null));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testPayRequestContainsDynamicId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        payService.pay("130818341921600584", "100", "咖啡", "cashier01", null);

        // 验证请求体包含 dynamic_id
        verify(httpClient).execute(anyString(), contains("130818341921600584"), anyString(), anyString());
    }

    @Test
    void testPayRequestContainsAmount() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        payService.pay("code", "9999", "测试", "op", null);

        // 金额以分为单位
        verify(httpClient).execute(anyString(), contains("9999"), anyString(), anyString());
    }

    @Test
    void testPayRequestContainsSubject() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        payService.pay("code", "100", "星巴克咖啡", "op", null);

        verify(httpClient).execute(anyString(), contains("星巴克咖啡"), anyString(), anyString());
    }

    @Test
    void testPayUsesTerminalLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        payService.pay("code", "100", "test", "op", null);

        verify(httpClient).execute(anyString(), anyString(), eq("terminal001"), eq("terminalkey001"));
    }

    @Test
    void testPayWithNotifyUrl() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        payService.pay("code", "100", "test", "op", "https://example.com/notify");

        verify(httpClient).execute(anyString(), contains("https://example.com/notify"), anyString(), anyString());
    }

    // ========== PAY_SUCCESS 但非最终状态的场景 ==========

    @Test
    void testPaySuccessButNonFinalStatusTriggersPolling() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_SUCCESS",
                        "data":{"order_status":"CREATED","client_sn":"sn999"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(pollResult);

        SqbResponse result = payService.pay("code", "100", "test", "op", null);

        assertEquals("PAID", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }
}
