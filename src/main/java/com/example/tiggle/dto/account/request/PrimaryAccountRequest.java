package com.example.tiggle.dto.account.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "주 계좌 등록 요청 DTO")
public class PrimaryAccountRequest {
    
    @Schema(description = "계좌번호", example = "0888367052216382", required = true)
    private String accountNo;
    
    @Schema(description = "계좌 검증 토큰", example = "abc123-def456-ghi789", required = true)
    private String verificationToken;
}