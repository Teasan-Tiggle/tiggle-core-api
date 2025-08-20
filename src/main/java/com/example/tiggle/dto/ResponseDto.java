package com.example.tiggle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {

    private Boolean result;
    private T data;
    private String message;

    public ResponseDto(Boolean result) {
        this.result = result;
    }

    public ResponseDto(Boolean result, String message) {
        this.result = result;
        this.message = message;
    }
}