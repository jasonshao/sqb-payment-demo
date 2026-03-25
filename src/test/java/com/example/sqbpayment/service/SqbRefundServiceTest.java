package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.RefundCommand;
import com.example.sqbpayment.model.request.RefundRequest;
import com.example.sqbpayment.util.ClientSnGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.sqbpayment.sdk.exception.SqbApiConnectionException;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqbRefundServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbApiTemplate apiTemplate;

    @Mock
    private SqbQueryService queryService;

    @Mock
    private ClientSnGenerator clientSnGenerator;

    private SqbConfig config;
    private SqbRefundService refundService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        lenient().when(clientSnGenerator.generateRefundNo()).thenReturn("REF20260320000000000001");

        refundService = new SqbRefundService(config, apiTemplate, queryService, clientSnGenerator);
    }

    private RefundCommand refundCommand(String sn, String clientSn, long refundAmount, String operator, String reason) {
        return new RefundCommand(sn, clientSn, refundAmount, operator, reason);
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
        when(apiTemplate.call(eq("/upay/v2/refund"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = refundService.refund(refundCommand("789284025", null, 100, "cashier01", "顾客要求退款")).join();

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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = refundService.refund(refundCommand(null, "order001", 50, "cashier01", null)).join();

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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = refundService.refund(refundCommand("sn001", null, 999, "op", null)).join();

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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED"}}}
                """));
        when(queryService.pollByClientSn("order001")).thenReturn(CompletableFuture.completedFuture(pollResult));

        // 使用 clientSn 查询（sn 为 null）
        SqbResponse result = refundService.refund(refundCommand(null, "order001", 100, "op", null)).join();

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
        when(apiTemplate.call(eq("/upay/v2/refund"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED","sn":"sn001"}}}
                """));
        when(queryService.pollBySn("sn001")).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = refundService.refund(refundCommand("sn001", null, 100, "op", null)).join();

        assertEquals("REFUNDED", result.getOrderStatus());
        verify(queryService).pollBySn("sn001");
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"REFUNDED"}}}
                """));
        when(queryService.pollByClientSn("order002")).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = refundService.refund(refundCommand(null, "order002", 100, "op", null)).join();

        assertEquals("REFUNDED", result.getOrderStatus());
    }

    // ========== 通信失败 ==========

    @Test
    void testRefundCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"500","error_message":"服务器错误"}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = refundService.refund(refundCommand("sn", null, 100, "op", null)).join();

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testRefundNetworkException() throws Exception {
        when(apiTemplate.call(anyString(), any()))
                .thenThrow(new SqbApiConnectionException("连接超时", new IOException("连接超时")));

        assertThrows(SqbApiConnectionException.class,
                () -> refundService.refund(refundCommand("sn", null, 100, "op", null)));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testRefundRequestContainsRefundAmount() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        refundService.refund(refundCommand("sn001", null, 250, "op", null));

        ArgumentCaptor<RefundRequest> captor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("250", captor.getValue().getRefundAmount());
    }

    @Test
    void testRefundRequestContainsRefundRequestNo() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        refundService.refund(refundCommand("sn001", null, 100, "op", "退款原因"));

        ArgumentCaptor<RefundRequest> captor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertTrue(captor.getValue().getRefundRequestNo().startsWith("REF"));
    }

    @Test
    void testRefundRequestContainsRefundReason() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        refundService.refund(refundCommand("sn001", null, 100, "op", "顾客要求退款"));

        ArgumentCaptor<RefundRequest> captor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("顾客要求退款", captor.getValue().getRefundReason());
    }

    @Test
    void testRefundRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"REFUND_SUCCESS","data":{"order_status":"REFUNDED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        refundService.refund(refundCommand("sn001", null, 100, "op", null));

        ArgumentCaptor<RefundRequest> captor = ArgumentCaptor.forClass(RefundRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }

    // ========== sn/clientSn 校验 ==========

    @Test
    void testRefundMissingSnAndClientSn() {
        assertThrows(IllegalArgumentException.class,
                () -> refundService.refund(refundCommand(null, null, 100, "op", null)));
    }
}
