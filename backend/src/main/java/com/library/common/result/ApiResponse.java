package com.library.common.result;

/**
 * 统一响应结构。code = 0 表示成功，其余见 {@link com.library.common.exception.ErrorCode}。
 */
public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, System.currentTimeMillis());
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, System.currentTimeMillis());
    }
}
