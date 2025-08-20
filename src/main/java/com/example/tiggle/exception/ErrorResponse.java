package com.example.tiggle.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {

    private final Boolean result;
    private final String message;

    public ErrorResponse(Boolean result, String message) {
        this.result = result;
        this.message = message;
    }
}