package com.example.tiggle.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "공통 API 응답 DTO")
public class ApiResponse<T> {
    
    @Schema(description = "결과", example = "true")
    private boolean result;
    
    @Schema(description = "에러 메시지 (실패 시)")
    private String message;
    
    @Schema(description = "응답 데이터")
    private T data;
    
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