package com.example.sqbpayment.controller;

import com.example.sqbpayment.model.ApiResult;
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
 *
 * 集中处理所有 Controller 的异常，消除各 Controller 中重复的 try-catch。
 * 将异常映射为统一的 ApiResult 响应格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation 校验失败（@Valid 触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResult.fail(message));
    }

    /**
     * 业务参数校验失败（手动抛出的 IllegalArgumentException）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResult.fail(e.getMessage()));
    }

    /**
     * 收钱吧 API 通信失败
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResult<Void>> handleIoException(IOException e) {
        log.error("收钱吧服务通信失败", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResult.fail("收钱吧服务通信失败: " + e.getMessage()));
    }

    /**
     * 异步轮询被中断
     */
    @ExceptionHandler(InterruptedException.class)
    public ResponseEntity<ApiResult<Void>> handleInterrupted(InterruptedException e) {
        Thread.currentThread().interrupt();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail("请求被中断"));
    }
}
