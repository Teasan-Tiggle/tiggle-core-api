package com.example.tiggle.controller.account;

import com.example.tiggle.dto.account.request.OneWonVerificationRequest;
import com.example.tiggle.dto.account.request.OneWonVerificationValidateRequest;
import com.example.tiggle.dto.account.request.PrimaryAccountRequest;
import com.example.tiggle.dto.account.response.OneWonVerificationResponse;
import com.example.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.service.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "계좌 관리", description = "계좌 관련 API")
public class AccountController {
    
    private final AccountService accountService;
    
    @Operation(summary = "1원 송금 요청", description = "계좌번호로 1원 송금을 통한 계좌 인증을 수행합니다")
    @PostMapping("/transfer-verification")
    public Mono<OneWonVerificationResponse> sendOneWonVerification(
            @Parameter(description = "암호화된 사용자 키", required = true)
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @RequestBody OneWonVerificationRequest request) {
        
        return accountService.sendOneWonVerification(encryptedUserKey, request.getAccountNo());
    }
    
    @Operation(summary = "1원 송금 인증 코드 검증", description = "1원 송금으로 받은 인증 코드를 검증하고 검증 토큰을 발급합니다")
    @PostMapping("/transfer-confirmation")
    public Mono<OneWonVerificationValidateResponse> validateOneWonAuth(
            @Parameter(description = "암호화된 사용자 키", required = true)
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId,
            @RequestBody OneWonVerificationValidateRequest request) {
        
        return accountService.validateOneWonAuth(encryptedUserKey, request.getAccountNo(), request.getAuthCode(), userId);
    }
    
    @Operation(summary = "주 계좌 등록", description = "검증된 계좌를 주 계좌로 등록합니다")
    @PostMapping("/primary")
    public Mono<ApiResponse<Void>> registerPrimaryAccount(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId,
            @RequestBody PrimaryAccountRequest request) {
        
        return accountService.registerPrimaryAccount(
                request.getAccountNo(), request.getVerificationToken(), userId);
    }
    
    @Operation(summary = "주 계좌 조회", description = "등록된 주 계좌 정보를 조회합니다")
    @GetMapping("/primary")
    public Mono<ApiResponse<PrimaryAccountInfoDto>> getPrimaryAccount(
            @Parameter(description = "암호화된 사용자 키", required = true)
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId) {
        
        return accountService.getPrimaryAccount(encryptedUserKey, userId);
    }
}