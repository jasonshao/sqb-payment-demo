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
class SqbQueryServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbHttpClient httpClient;

    private SqbConfig config;
    private SqbQueryService queryService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        when(httpClient.getObjectMapper()).thenReturn(new ObjectMapper());

        queryService = new SqbQueryService(config, httpClient);
    }

    // ========== 单次查询测试 ==========

    @Test
    void testQueryByClientSn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"order001",
                            "order_status":"PAID",
                            "total_amount":"100",
                            "net_amount":"97"
                        }
                    }
                }
                """;
        when(httpClient.execute(contains("/upay/v2/query"), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = queryService.queryByClientSn("order001");

        assertTrue(result.isCommunicationSuccess());
        assertEquals("PAID", result.getOrderStatus());
        assertEquals("100", result.getTotalAmount());

        // 验证请求体包含 client_sn
        verify(httpClient).execute(anyString(), contains("order001"), anyString(), anyString());
    }

    @Test
    void testQueryBySn() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"SUCCESS",
                        "data":{"sn":"789284025","order_status":"PAID"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        SqbResponse result = queryService.queryBySn("789284025");

        assertEquals("PAID", result.getOrderStatus());
        verify(httpClient).execute(anyString(), contains("789284025"), anyString(), anyString());
    }

    @Test
    void testQueryUsesTerminalLevelSigning() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        queryService.queryByClientSn("order001");

        verify(httpClient).execute(anyString(), anyString(), eq("terminal001"), eq("terminalkey001"));
    }

    @Test
    void testQueryNetworkException() throws Exception {
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IOException("网络超时"));

        assertThrows(IOException.class, () -> queryService.queryByClientSn("order001"));
    }

    @Test
    void testQueryRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(responseJson));

        queryService.queryByClientSn("order001");

        verify(httpClient).execute(anyString(), contains("terminal001"), anyString(), anyString());
    }

    // ========== 轮询测试 ==========

    @Test
    void testPollReturnsImmediatelyOnFinalStatus() throws Exception {
        String paidResponse = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"SUCCESS",
                        "data":{"order_status":"PAID","client_sn":"order001"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(paidResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        // 第一次查询就是最终状态，只应调用一次
        verify(httpClient, times(1)).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testPollReturnOnPayCanceled() throws Exception {
        String canceledResponse = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"SUCCESS",
                        "data":{"order_status":"PAY_CANCELED"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(canceledResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(httpClient, times(1)).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testPollReturnOnRefunded() throws Exception {
        String refundedResponse = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"SUCCESS",
                        "data":{"order_status":"REFUNDED"}
                    }
                }
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(refundedResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("REFUNDED", result.getOrderStatus());
    }

    @Test
    void testPollContinuesOnNonFinalStatus() throws Exception {
        String createdResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"CREATED"}}}
                """;
        String paidResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;

        // 第一次返回 CREATED，第二次返回 PAID
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(createdResponse))
                .thenReturn(MAPPER.readTree(paidResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        verify(httpClient, times(2)).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testPollContinuesOnCommunicationFailure() throws Exception {
        String failResponse = """
                {"result_code":"500","error_message":"服务器错误"}
                """;
        String paidResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;

        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(failResponse))
                .thenReturn(MAPPER.readTree(paidResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        verify(httpClient, times(2)).execute(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testPollContinuesOnPayError() throws Exception {
        String errorResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAY_ERROR"}}}
                """;
        String paidResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;

        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(errorResponse))
                .thenReturn(MAPPER.readTree(paidResponse));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
    }

    @Test
    void testPollInterrupted() throws Exception {
        String createdResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"CREATED"}}}
                """;
        when(httpClient.execute(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(MAPPER.readTree(createdResponse));

        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> queryService.pollByClientSn("order001"));
        // 清除中断状态
        Thread.interrupted();
    }
}
