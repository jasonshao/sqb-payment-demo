package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.QueryRequest;
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
class SqbQueryServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbApiTemplate apiTemplate;

    private SqbConfig config;
    private SqbQueryService queryService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        queryService = new SqbQueryService(config, apiTemplate);
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
        when(apiTemplate.call(eq("/upay/v2/query"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = queryService.queryByClientSn("order001");

        assertTrue(result.isCommunicationSuccess());
        assertEquals("PAID", result.getOrderStatus());
        assertEquals("100", result.getTotalAmount());

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("order001", captor.getValue().getClientSn());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = queryService.queryBySn("789284025");

        assertEquals("PAID", result.getOrderStatus());

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("789284025", captor.getValue().getSn());
    }

    @Test
    void testQueryRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        queryService.queryByClientSn("order001");

        ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }

    @Test
    void testQueryNetworkException() throws Exception {
        when(apiTemplate.call(anyString(), any()))
                .thenThrow(new IOException("网络超时"));

        assertThrows(IOException.class, () -> queryService.queryByClientSn("order001"));
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(paidResponse)));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        verify(apiTemplate, times(1)).call(anyString(), any());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(canceledResponse)));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(apiTemplate, times(1)).call(anyString(), any());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(refundedResponse)));

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

        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(createdResponse)))
                .thenReturn(new SqbResponse(MAPPER.readTree(paidResponse)));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        verify(apiTemplate, times(2)).call(anyString(), any());
    }

    @Test
    void testPollContinuesOnCommunicationFailure() throws Exception {
        String failResponse = """
                {"result_code":"500","error_message":"服务器错误"}
                """;
        String paidResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;

        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(failResponse)))
                .thenReturn(new SqbResponse(MAPPER.readTree(paidResponse)));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
        verify(apiTemplate, times(2)).call(anyString(), any());
    }

    @Test
    void testPollContinuesOnPayError() throws Exception {
        String errorResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAY_ERROR"}}}
                """;
        String paidResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"PAID"}}}
                """;

        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(errorResponse)))
                .thenReturn(new SqbResponse(MAPPER.readTree(paidResponse)));

        SqbResponse result = queryService.pollByClientSn("order001").join();

        assertEquals("PAID", result.getOrderStatus());
    }

    @Test
    void testPollInterrupted() throws Exception {
        String createdResponse = """
                {"result_code":"200","biz_response":{"result_code":"SUCCESS","data":{"order_status":"CREATED"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(createdResponse)));

        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> queryService.pollByClientSn("order001"));
        // 清除中断状态
        Thread.interrupted();
    }
}
