package com.example.sqbpayment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应包装
 *
 * @param success 业务是否成功
 * @param message 错误信息（成功时为 null）
 * @param data    响应数据（失败时为 null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(boolean success, String message, T data) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, null, data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(false, message, null);
    }
}
