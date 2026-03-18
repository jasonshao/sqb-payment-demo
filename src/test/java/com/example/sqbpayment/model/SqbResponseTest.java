package com.example.sqbpayment.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqbResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SqbResponse createResponse(String json) throws Exception {
        return new SqbResponse(MAPPER.readTree(json));
    }

    // ========== 通信层测试 ==========

    @Test
    void testCommunicationSuccess() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{}}}
                """);
        assertTrue(resp.isCommunicationSuccess());
        assertEquals("200", resp.getResultCode());
    }

    @Test
    void testCommunicationFailure() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"400","error_code":"INVALID_PARAMS","error_message":"参数错误"}
                """);
        assertFalse(resp.isCommunicationSuccess());
        assertEquals("400", resp.getResultCode());
        assertEquals("INVALID_PARAMS", resp.getErrorCode());
        assertEquals("参数错误", resp.getErrorMessage());
    }

    // ========== 业务层测试 ==========

    @Test
    void testBizResultCode() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{}}}
                """);
        assertEquals("PAY_SUCCESS", resp.getBizResultCode());
    }

    @Test
    void testBizError() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"200","biz_response":{"result_code":"PAY_FAIL","error_code":"INVALID_BARCODE","error_message":"无效付款码"}}
                """);
        assertEquals("PAY_FAIL", resp.getBizResultCode());
        assertEquals("INVALID_BARCODE", resp.getBizErrorCode());
        assertEquals("无效付款码", resp.getBizErrorMessage());
    }

    // ========== 支付响应数据测试 ==========

    @Test
    void testPaySuccessResponse() throws Exception {
        SqbResponse resp = createResponse("""
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_SUCCESS",
                        "data":{
                            "sn":"7892840250140845",
                            "client_sn":"20230615143052001",
                            "trade_no":"2023061522001456781234567890",
                            "status":"PAID",
                            "order_status":"PAID",
                            "total_amount":"100",
                            "net_amount":"97",
                            "finish_time":"1686816652000"
                        }
                    }
                }
                """);

        assertEquals("PAID", resp.getOrderStatus());
        assertEquals("7892840250140845", resp.getSn());
        assertEquals("20230615143052001", resp.getClientSn());
        assertEquals("2023061522001456781234567890", resp.getTradeNo());
        assertEquals("100", resp.getTotalAmount());
        assertEquals("97", resp.getNetAmount());
        assertEquals("1686816652000", resp.getFinishTime());
    }

    // ========== 激活/签到响应测试 ==========

    @Test
    void testActivateResponse() throws Exception {
        SqbResponse resp = createResponse("""
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"ACTIVATE_SUCCESS",
                        "data":{
                            "terminal_sn":"10298371039",
                            "terminal_key":"68d499beda5f72116592f5c527465656"
                        }
                    }
                }
                """);

        assertEquals("ACTIVATE_SUCCESS", resp.getBizResultCode());
        assertEquals("10298371039", resp.getTerminalSn());
        assertEquals("68d499beda5f72116592f5c527465656", resp.getTerminalKey());
    }

    @Test
    void testCheckinResponse() throws Exception {
        SqbResponse resp = createResponse("""
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"TERMINAL_CHECKIN_SUCCESS",
                        "data":{
                            "terminal_sn":"10298371039",
                            "terminal_key":"a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
                        }
                    }
                }
                """);

        assertEquals("TERMINAL_CHECKIN_SUCCESS", resp.getBizResultCode());
        assertEquals("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4", resp.getTerminalKey());
    }

    // ========== 退款响应测试 ==========

    @Test
    void testRefundResponse() throws Exception {
        SqbResponse resp = createResponse("""
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"REFUND_SUCCESS",
                        "data":{
                            "sn":"7892840250140845",
                            "client_sn":"20230615143052001",
                            "status":"REFUNDED",
                            "order_status":"REFUNDED",
                            "total_amount":"100",
                            "net_amount":"0",
                            "refunded_amount":"100"
                        }
                    }
                }
                """);

        assertEquals("REFUNDED", resp.getOrderStatus());
        assertEquals("100", resp.getRefundedAmount());
        assertEquals("0", resp.getNetAmount());
    }

    @Test
    void testPartialRefundResponse() throws Exception {
        SqbResponse resp = createResponse("""
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
                """);

        assertEquals("PARTIAL_REFUNDED", resp.getOrderStatus());
        assertEquals("50", resp.getRefundedAmount());
    }

    // ========== 边界情况 ==========

    @Test
    void testMissingFields() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"200","biz_response":{"result_code":"PAY_SUCCESS","data":{}}}
                """);

        assertEquals("", resp.getOrderStatus());
        assertEquals("", resp.getSn());
        assertEquals("", resp.getClientSn());
        assertEquals("", resp.getTradeNo());
        assertEquals("", resp.getTotalAmount());
    }

    @Test
    void testMissingBizResponse() throws Exception {
        SqbResponse resp = createResponse("""
                {"result_code":"500"}
                """);

        assertEquals("", resp.getBizResultCode());
        assertEquals("", resp.getOrderStatus());
    }

    @Test
    void testOrderStatusFallbackToStatus() throws Exception {
        // 当 order_status 不存在时，应回退到 status 字段
        SqbResponse resp = createResponse("""
                {
                    "result_code":"200",
                    "biz_response":{
                        "result_code":"PAY_SUCCESS",
                        "data":{"status":"PAID"}
                    }
                }
                """);

        assertEquals("PAID", resp.getOrderStatus());
    }

    @Test
    void testGetRawResponse() throws Exception {
        String json = "{\"result_code\":\"200\"}";
        SqbResponse resp = createResponse(json);
        assertNotNull(resp.getRawResponse());
        assertEquals("200", resp.getRawResponse().path("result_code").asText());
    }

    @Test
    void testToString() throws Exception {
        SqbResponse resp = createResponse("{\"result_code\":\"200\"}");
        assertNotNull(resp.toString());
        assertTrue(resp.toString().contains("200"));
    }
}
