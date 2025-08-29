package com.ssafy.tiggle.dto.account.response;

import com.ssafy.tiggle.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "1원 송금 인증 코드 검증 응답 DTO")
public class OneWonVerificationValidateResponse extends ApiResponse<AccountVerificationToken> {
    
    private OneWonVerificationValidateResponse(boolean result, String message, AccountVerificationToken data) {
        super(result, message, data);
    }
    
    public static OneWonVerificationValidateResponse success(String verificationToken) {
        return new OneWonVerificationValidateResponse(true, null, new AccountVerificationToken(verificationToken));
    }
    
    public static OneWonVerificationValidateResponse failure(String message) {
        return new OneWonVerificationValidateResponse(false, message, null);
    }
}