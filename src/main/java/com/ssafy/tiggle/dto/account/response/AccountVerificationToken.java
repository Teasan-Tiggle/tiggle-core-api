package com.ssafy.tiggle.dto.account.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "1원 송금 인증 코드 검증 응답 데이터")
public class AccountVerificationToken {
    
    @Schema(description = "검증 토큰")
    private String verificationToken;
}