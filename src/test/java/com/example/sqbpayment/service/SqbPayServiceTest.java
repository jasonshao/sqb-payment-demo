package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.PayCommand;
import com.example.sqbpayment.model.request.PayRequest;
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
class SqbPayServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbApiTemplate apiTemplate;

    @Mock
    private SqbQueryService queryService;

    @Mock
    private ClientSnGenerator clientSnGenerator;

    private SqbConfig config;
    private SqbPayService payService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        when(clientSnGenerator.generate()).thenReturn("20260320000000000001");

        payService = new SqbPayService(config, apiTemplate, queryService, clientSnGenerator);
    }

    private PayCommand payCommand(String dynamicId, long totalAmount, String subject, String operator, String notifyUrl) {
        return new PayCommand(dynamicId, totalAmount, subject, operator, notifyUrl);
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
        when(apiTemplate.call(eq("/upay/v2/pay"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = payService.pay(payCommand("130818341921600584", 100, "测试商品", "cashier01", null)).join();

        assertEquals("PAID", result.getOrderStatus());
        assertEquals("100", result.getTotalAmount());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = payService.pay(payCommand("invalid_code", 100, "测试", "op", null)).join();

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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(payResponseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID","client_sn":"sn001"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = payService.pay(payCommand("code", 100, "test", "op", null)).join();

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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAY_CANCELED"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = payService.pay(payCommand("code", 100, "test", "op", null)).join();

        assertEquals("PAY_CANCELED", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }

    // ========== 通信失败场景 ==========

    @Test
    void testPayCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"400","error_code":"ILLEGAL_SIGN","error_message":"签名错误"}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = payService.pay(payCommand("code", 100, "test", "op", null)).join();

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testPayNetworkException() throws Exception {
        when(apiTemplate.call(anyString(), any()))
                .thenThrow(new SqbApiConnectionException("连接超时", new IOException("连接超时")));

        assertThrows(SqbApiConnectionException.class,
                () -> payService.pay(payCommand("code", 100, "test", "op", null)));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testPayRequestContainsDynamicId() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        payService.pay(payCommand("130818341921600584", 100, "咖啡", "cashier01", null));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(apiTemplate).call(eq("/upay/v2/pay"), captor.capture());
        assertEquals("130818341921600584", captor.getValue().getDynamicId());
    }

    @Test
    void testPayRequestContainsAmount() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        payService.pay(payCommand("code", 9999, "测试", "op", null));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("9999", captor.getValue().getTotalAmount());
    }

    @Test
    void testPayRequestContainsSubject() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        payService.pay(payCommand("code", 100, "星巴克咖啡", "op", null));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("星巴克咖啡", captor.getValue().getSubject());
    }

    @Test
    void testPayRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        payService.pay(payCommand("code", 100, "test", "op", null));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }

    @Test
    void testPayWithNotifyUrl() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        payService.pay(payCommand("code", 100, "test", "op", "https://example.com/notify"));

        ArgumentCaptor<PayRequest> captor = ArgumentCaptor.forClass(PayRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("https://example.com/notify", captor.getValue().getNotifyUrl());
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
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{"order_status":"PAID"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = payService.pay(payCommand("code", 100, "test", "op", null)).join();

        assertEquals("PAID", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }
}
