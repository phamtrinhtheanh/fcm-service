package com.noti.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private boolean error;
    private String message;
    private String errorCode;
    private T data;

    public BaseResponse(boolean error, String message) {
        this.error = error;
        this.message = message;
    }

    public BaseResponse(boolean error, String message, T data) {
        this.error = error;
        this.message = message;
        this.data = data;
    }

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(false, "OK", data);
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(false, message, data);
    }

    public static BaseResponse<Void> success() {
        return new BaseResponse<>(false, "OK");
    }

    public static BaseResponse<Void> error(String message) {
        return new BaseResponse<>(true, message);
    }
}
