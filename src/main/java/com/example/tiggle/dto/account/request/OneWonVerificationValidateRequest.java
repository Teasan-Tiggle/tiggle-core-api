package com.example.tiggle.dto.account.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "1원 송금 인증 코드 검증 요청 DTO")
public class OneWonVerificationValidateRequest {

    @NotNull
    @Schema(description = "계좌번호", example = "0888367052216382", required = true)
    private String accountNo;

    @NotNull
    @Schema(description = "인증 코드", example = "1234", required = true)
    private String authCode;
}