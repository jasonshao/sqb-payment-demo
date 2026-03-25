package com.example.sqbpayment.service;

import com.example.sqbpayment.config.SqbConfig;
import com.example.sqbpayment.model.SqbResponse;
import com.example.sqbpayment.model.request.PrecreateCommand;
import com.example.sqbpayment.model.request.PrecreateRequest;
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
class SqbPrecreateServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SqbApiTemplate apiTemplate;

    @Mock
    private SqbQueryService queryService;

    @Mock
    private ClientSnGenerator clientSnGenerator;

    private SqbConfig config;
    private SqbPrecreateService precreateService;

    @BeforeEach
    void setUp() {
        config = new SqbConfig();
        config.setApiBase("https://vsi-api.shouqianba.com");
        config.setTerminalSn("terminal001");
        config.setTerminalKey("terminalkey001");

        when(clientSnGenerator.generate()).thenReturn("20260320000000000001");

        precreateService = new SqbPrecreateService(config, apiTemplate, queryService, clientSnGenerator);
    }

    private PrecreateCommand precreateCommand(String payway, long totalAmount, String subject, String operator, String notifyUrl) {
        return new PrecreateCommand(payway, totalAmount, subject, operator, notifyUrl);
    }

    // ========== 预创建成功场景 ==========

    @Test
    void testPrecreateSuccess() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PRECREATE_SUCCESS",
                        "data":{
                            "sn":"789284025",
                            "client_sn":"20260320000000000001",
                            "order_status":"CREATED",
                            "qr_code":"https://qr.alipay.com/test123",
                            "total_amount":"100"
                        }
                    }
                }
                """;
        when(apiTemplate.call(eq("/upay/v2/precreate"), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = precreateService.precreate(precreateCommand("4", 100, "测试商品", "cashier01", null)).join();

        assertEquals("https://qr.alipay.com/test123", result.getQrCode());
        assertEquals("100", result.getTotalAmount());
        verifyNoInteractions(queryService);
    }

    // ========== 预创建失败场景 ==========

    @Test
    void testPrecreateFail() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PRECREATE_FAIL",
                        "error_code":"INVALID_PARAMS",
                        "error_message":"参数错误"
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = precreateService.precreate(precreateCommand("4", 100, "测试", "op", null)).join();

        assertEquals("PRECREATE_FAIL", result.getBizResultCode());
        verifyNoInteractions(queryService);
    }

    // ========== 预创建进行中 -> 轮询场景 ==========

    @Test
    void testPrecreateInProgressTriggersPolling() throws Exception {
        String responseJson = """
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PRECREATE_IN_PROGRESS",
                        "data":{"order_status":"CREATED","client_sn":"sn001"}
                    }
                }
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse pollResult = new SqbResponse(MAPPER.readTree("""
                {"result_code":"200","biz_response":{"result_code":"PRECREATE_SUCCESS","data":{"order_status":"PAID","qr_code":"https://qr.alipay.com/test"}}}
                """));
        when(queryService.pollByClientSn(anyString())).thenReturn(CompletableFuture.completedFuture(pollResult));

        SqbResponse result = precreateService.precreate(precreateCommand("3", 100, "test", "op", null)).join();

        assertEquals("PAID", result.getOrderStatus());
        verify(queryService).pollByClientSn(anyString());
    }

    // ========== 通信失败场景 ==========

    @Test
    void testPrecreateCommunicationFailure() throws Exception {
        String responseJson = """
                {"result_code":"400","error_code":"ILLEGAL_SIGN","error_message":"签名错误"}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        SqbResponse result = precreateService.precreate(precreateCommand("3", 100, "test", "op", null)).join();

        assertFalse(result.isCommunicationSuccess());
        verifyNoInteractions(queryService);
    }

    @Test
    void testPrecreateNetworkException() throws Exception {
        when(apiTemplate.call(anyString(), any()))
                .thenThrow(new SqbApiConnectionException("连接超时", new IOException("连接超时")));

        assertThrows(SqbApiConnectionException.class,
                () -> precreateService.precreate(precreateCommand("3", 100, "test", "op", null)));
    }

    // ========== 请求参数验证 ==========

    @Test
    void testPrecreateRequestContainsPayway() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PRECREATE_SUCCESS","data":{"order_status":"CREATED","qr_code":"https://qr.test"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        precreateService.precreate(precreateCommand("3", 100, "微信支付", "cashier01", null));

        ArgumentCaptor<PrecreateRequest> captor = ArgumentCaptor.forClass(PrecreateRequest.class);
        verify(apiTemplate).call(eq("/upay/v2/precreate"), captor.capture());
        assertEquals("3", captor.getValue().getPayway());
    }

    @Test
    void testPrecreateRequestContainsTerminalSn() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PRECREATE_SUCCESS","data":{"order_status":"CREATED","qr_code":"https://qr.test"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        precreateService.precreate(precreateCommand("4", 100, "test", "op", null));

        ArgumentCaptor<PrecreateRequest> captor = ArgumentCaptor.forClass(PrecreateRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("terminal001", captor.getValue().getTerminalSn());
    }

    @Test
    void testPrecreateRequestContainsAmount() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PRECREATE_SUCCESS","data":{"order_status":"CREATED","qr_code":"https://qr.test"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        precreateService.precreate(precreateCommand("3", 9999, "测试", "op", null));

        ArgumentCaptor<PrecreateRequest> captor = ArgumentCaptor.forClass(PrecreateRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("9999", captor.getValue().getTotalAmount());
    }

    @Test
    void testPrecreateWithNotifyUrl() throws Exception {
        String responseJson = """
                {"result_code":"200","biz_response":{"result_code":"PRECREATE_SUCCESS","data":{"order_status":"CREATED","qr_code":"https://qr.test"}}}
                """;
        when(apiTemplate.call(anyString(), any()))
                .thenReturn(new SqbResponse(MAPPER.readTree(responseJson)));

        precreateService.precreate(precreateCommand("3", 100, "test", "op", "https://example.com/notify"));

        ArgumentCaptor<PrecreateRequest> captor = ArgumentCaptor.forClass(PrecreateRequest.class);
        verify(apiTemplate).call(anyString(), captor.capture());
        assertEquals("https://example.com/notify", captor.getValue().getNotifyUrl());
    }
}
