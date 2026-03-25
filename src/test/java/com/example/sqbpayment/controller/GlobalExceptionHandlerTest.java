package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.sdk.exception.SqbApiConnectionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testUnexpectedExceptionReturns500WithApiResult() {
        RuntimeException ex = new RuntimeException("something unexpected");

        ResponseEntity<ApiResult<Void>> response = handler.handleUnexpectedException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("服务内部错误", response.getBody().message());
    }

    @Test
    void testIllegalArgumentReturns400() {
        IllegalArgumentException ex = new IllegalArgumentException("bad param");

        ResponseEntity<ApiResult<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("bad param", response.getBody().message());
    }

    @Test
    void testSqbConnectionExceptionReturns502() {
        SqbApiConnectionException ex = new SqbApiConnectionException("连接失败", new IOException("timeout"));
        ResponseEntity<ApiResult<Void>> response = handler.handleConnectionException(ex);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
    }
}
