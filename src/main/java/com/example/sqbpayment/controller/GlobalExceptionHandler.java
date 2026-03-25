package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
import com.example.sqbpayment.sdk.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResult.fail(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
    }

    @ExceptionHandler(SqbApiConnectionException.class)
    public ResponseEntity<ApiResult<Void>> handleConnectionException(SqbApiConnectionException e) {
        log.error("收钱吧服务通信失败", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.fail("收钱吧服务通信失败: " + e.getMessage()));
    }

    @ExceptionHandler(SqbAuthenticationException.class)
    public ResponseEntity<ApiResult<Void>> handleAuthenticationException(SqbAuthenticationException e) {
        log.error("收钱吧认证失败", e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.fail("认证失败: " + e.getMessage()));
    }

    @ExceptionHandler(SqbSignatureVerificationException.class)
    public ResponseEntity<ApiResult<Void>> handleSignatureException(SqbSignatureVerificationException e) {
        log.warn("签名验证失败", e);
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.fail("签名验证失败"));
    }

    @ExceptionHandler(SqbRateLimitException.class)
    public ResponseEntity<ApiResult<Void>> handleRateLimitException(SqbRateLimitException e) {
        log.warn("请求限流", e);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResult.fail(e.getUserMessage()));
    }

    @ExceptionHandler(SqbException.class)
    public ResponseEntity<ApiResult<Void>> handleSqbException(SqbException e) {
        log.error("收钱吧 SDK 异常", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.fail(e.getUserMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResult<Void>> handleIoException(IOException e) {
        log.error("收钱吧服务通信失败", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.fail("收钱吧服务通信失败: " + e.getMessage()));
    }

    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ApiResult<Void>> handleInterrupted(InterruptedException e) {
        Thread.currentThread().interrupt();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("请求被中断"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleUnexpectedException(Exception e) {
        log.error("未预期的服务异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("服务内部错误"));
    }
}
