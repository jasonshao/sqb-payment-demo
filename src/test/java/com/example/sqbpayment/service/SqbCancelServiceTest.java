package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.CancelCommand;
import com.example.sqbpayment.model.request.CancelRequest;
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
class SqbCancelServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbApiTemplate apiTemplate;

    @Mock
    private SqbQueryService queryService;

    private SqbConfig config;
    private SqbCancelService cancelService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        cancelService = new SqbCancelService(config, apiTemplate, queryService);
    }

    private CancelCommand cancelCommand(String sn, String clientSn) {
        return new CancelCommand(sn, clientSn);
    }

    // ========== 撤单成功场景 ==========

    @Test
    void testCancelSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"order001",
                            "order_status":"PAY_CANCELED",
                            "total_amount":"100"
                        }
                    }
                }
                """;
        when(apiTemplate.call(eq("/upay/v2/cancel"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = cancelService.cancel(cancelCommand("789284025", null)).join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verifyNoInteractions(queryService);
    }

    // ========== 撤单失败场景 ==========

    @Test
    void testCancelFail() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_FAIL",
                        "error_code":"ORDER_NOT_EXIST",
                        "error_message":"订单不存在"
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = cancelService.cancel(cancelCommand("sn001", null)).join();

        assertEquals("CANCEL_FAIL", result.getBizResultCode());
        verifyNoInteractions(queryService);
    }

    // ========== CANCEL_ERROR -> 查询确认 ==========

    @Test
    void testCancelErrorTriggersQueryBySn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_ERROR",
                        "data":{"order_status":"CANCEL_ERROR","sn":"sn001"}
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAY_CANCELED","sn":"sn001"}}}
                """));
        when(queryService.pollBySn("sn001")).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = cancelService.cancel(cancelCommand("sn001", null)).join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(queryService).pollBySn("sn001");
    }

    @Test
    void testCancelErrorTriggersQueryByClientSn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_ERROR",
                        "data":{"order_status":"CANCEL_ERROR"}
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAY_CANCELED"}}}
                """));
        when(queryService.pollByClientSn("order001")).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = cancelService.cancel(cancelCommand(null, "order001")).join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(queryService).pollByClientSn("order001");
    }

    // ========== 通信失败 ==========

    @Test
    void testCancelCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"500","error_message":"服务器错误"}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = cancelService.cancel(cancelCommand("sn", null)).join();

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testCancelNetworkException() throws Exception {
        when(apiTemplate.call(anyString(), any()))
                .thenThrow(new SqbApiConnectionException("连接超时", new IOException("连接超时")));

        assertThrows(SqbApiConnectionException.class,
                () -> cancelService.cancel(cancelCommand("sn", null)));
    }

    // ========== sn/clientSn 校验 ==========

    @Test
    void testCancelMissingSnAndClientSn() {
        assertThrows(IllegalArgumentException.class,
                () -> cancelService.cancel(cancelCommand(null, null)));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testCancelPrefersSn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"CANCEL_ERROR",
                        "data":{"order_status":"CANCEL_ERROR"}
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAY_CANCELED"}}}
                """));
        when(queryService.pollBySn("sn001")).thenReturn(CompletableFuture.completedFuture(pollResult));

        cancelService.cancel(cancelCommand("sn001", "client001")).join();

        verify(queryService).pollBySn("sn001");
        verify(queryService, never()).pollByClientSn(anyString());
    }

    @Test
    void testCancelRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"CANCEL_SUCCESS","data":{"order_status":"PAY_CANCELED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        cancelService.cancel(cancelCommand("sn001", null));

        ArgumentCaptor<CancelRequest> captor = ArgumentCaptor.forClass(CancelRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }
}
