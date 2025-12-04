package com.software.logistic.common;

import lombok.Data;

@Data
public class ResponseResult<T> {
    private Integer code;
    private String message;
    private T data;

    private ResponseResult() {
    }

    private ResponseResult(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(0, "成功", null);
    }

    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(0, "成功", data);
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        return new ResponseResult<>(0, message, data);
    }

    public static <T> ResponseResult<T> error(Integer code, String message) {
        return new ResponseResult<>(code, message, null);
    }

    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(1000, message, null);
    }
}