package com.example.tiggle.dto.account.response;

import com.example.tiggle.dto.common.ApiResponse;

public class OneWonVerificationResponse extends ApiResponse<Void> {
    
    private OneWonVerificationResponse(boolean result, String message, Void data) {
        super(result, message, data);
    }
    
    public static OneWonVerificationResponse success() {
        return new OneWonVerificationResponse(true, null, null);
    }
    
    public static OneWonVerificationResponse failure(String message) {
        return new OneWonVerificationResponse(false, message, null);
    }
}