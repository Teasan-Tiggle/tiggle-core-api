package com.example.tiggle.dto;

import com.example.tiggle.dto.common.ApiResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDto<T> {

    private Boolean result;
    private T data;
    private String message;

    public ResponseDto(Boolean result) {
        this.result = result;
    }

    public ResponseDto(Boolean result, T data) {
        this.result = result;
        this.data = data;
    }

    public ResponseDto(Boolean result, String message) {
        this.result = result;
        this.message = message;
    }

    public ResponseDto(Boolean result, T data, String message) {
        this.result = result;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
}