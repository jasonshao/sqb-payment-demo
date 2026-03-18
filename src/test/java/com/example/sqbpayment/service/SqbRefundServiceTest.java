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
class SqbRefundServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbHttpClient httpClient;

    @Mock
    private SqbQueryService queryService;

    private SqbConfig config;
    private SqbRefundService refundService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        when(httpClient.getObjectMapper()).thenReturn(new ObjectMapper());

        refundService = new SqbRefundService(config, httpClient, queryService);
    }

    // ========== 全额退款成功 ==========

    @Test
    void testRefundFullSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"order001",
                            "order_status":"REFUNDED",
                            "total_amount":"100",
                            "refunded_amount":"100"
                        }
                    }
                }
                """;
        when(httpClient.execute(contains("/upay/v2/refund"), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = refundService.refund("789284025", null, "100", "cashier01", "顾客要求退款");

        assertEquals("REFUNDED", result.getOrderStatus());
        assertEquals("100", result.getRefundedAmount());
        verifyNoInteractions(queryService);
    }

    // ========== 部分退款成功 ==========

    @Test
    void testRefundPartialSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_SUCCESS",
                        "data":{
                            "order_status":"PARTIAL_REFUNDED",
                            "total_amount":"100",
                            "refunded_amount":"50"
                        }
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = refundService.refund(null, "order001", "50", "cashier01", null);

        assertEquals("PARTIAL_REFUNDED", result.getOrderStatus());
        assertEquals("50", result.getRefundedAmount());
    }

    // ========== 退款失败 ==========

    @Test
    void testRefundFail() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_FAIL",
                        "error_code":"REFUND_AMOUNT_EXCEEDED",
                        "error_message":"退款金额超过可退金额"
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = refundService.refund("sn001", null, "999", "op", null);

        assertEquals("REFUND_FAIL", result.getBizResultCode());
        verifyNoInteractions(queryService);
    }

    // ========== 退款异步轮询 ==========

    @Test
    void testRefundInProgressTriggersPollingByClientSn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_IN_PROGRESS",
                        "data":{"order_status":"REFUND_ERROR","client_sn":"order001"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED"}}}
                """));
        when(queryService.pollByClientSn("order001")).thenReturn(pollResult);

        // 使用 clientSn 查询（sn 为 null）
        SqbResponse result = refundService.refund(null, "order001", "100", "op", null);

        assertEquals("REFUNDED", result.getOrderStatus());
        verify(queryService).pollByClientSn("order001");
    }

    @Test
    void testRefundInProgressTriggersPollingBySn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_IN_PROGRESS",
                        "data":{"order_status":"REFUND_ERROR","sn":"sn001"}
                    }
                }
                """;
        when(httpClient.execute(contains("/upay/v2/refund"), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        // 使用 sn 查询时走 pollBySn 内部逻辑（queryBySn）
        String queryResponseJson = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED","sn":"sn001"}}}
                """;
        when(queryService.queryBySn("sn001")).thenReturn(new SqbResponse(MAPPER.readTree(queryResponseJson)));

        SqbResponse result = refundService.refund("sn001", null, "100", "op", null);

        assertEquals("REFUNDED", result.getOrderStatus());
        verify(queryService).queryBySn("sn001");
    }

    @Test
    void testRefundFailErrorTriggersPolling() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_FAIL_ERROR",
                        "data":{"order_status":"REFUND_ERROR"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED"}}}
                """));
        when(queryService.pollByClientSn("order002")).thenReturn(pollResult);

        SqbResponse result = refundService.refund(null, "order002", "100", "op", null);

        assertEquals("REFUNDED", result.getOrderStatus());
    }

    // ========== 通信失败 ==========

    @Test
    void testRefundCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"500","error_message":"服务器错误"}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = refundService.refund("sn", null, "100", "op", null);

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testRefundNetworkException() throws Exception {
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IOException("连接超时"));

        assertThrows(IOException.class,
                () -> refundService.refund("sn", null, "100", "op", null));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testRefundRequestContainsRefundAmount() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        refundService.refund("sn001", null, "250", "op", null);

        verify(httpClient).execute(anyString(), contains("250"), anyString(), anyString());
    }

    @Test
    void testRefundRequestContainsRefundRequestNo() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        refundService.refund("sn001", null, "100", "op", "退款原因");

        // 请求体应包含 refund_request_no（自动生成的，以 REF 开头）
        verify(httpClient).execute(anyString(), contains("REF"), anyString(), anyString());
    }

    @Test
    void testRefundRequestContainsRefundReason() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        refundService.refund("sn001", null, "100", "op", "顾客要求退款");

        verify(httpClient).execute(anyString(), contains("顾客要求退款"), anyString(), anyString());
    }

    @Test
    void testRefundUsesTerminalLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        refundService.refund("sn001", null, "100", "op", null);

        verify(httpClient).execute(anyString(), anyString(), eq("terminal001"), eq("terminalkey001"));
    }
}
