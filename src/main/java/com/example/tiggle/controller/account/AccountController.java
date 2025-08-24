package com.example.tiggle.controller.account;

import com.example.tiggle.dto.account.request.OneWonVerificationRequest;
import com.example.tiggle.dto.account.request.OneWonVerificationValidateRequest;
import com.example.tiggle.dto.account.request.PrimaryAccountRequest;
import com.example.tiggle.dto.account.response.AccountHolderInfoDto;
import com.example.tiggle.dto.account.response.OneWonVerificationResponse;
import com.example.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.account.response.TransactionHistoryResponse;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.service.account.AccountService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "계좌 관리", description = "계좌 관련 API")
public class AccountController {
    
    private final AccountService accountService;
    
    @Operation(summary = "1원 송금 요청", description = "계좌번호로 1원 송금을 통한 계좌 인증을 수행합니다")
    @PostMapping("/verification")
    public ResponseEntity<OneWonVerificationResponse> sendOneWonVerification(
            @RequestBody OneWonVerificationRequest request) {
        
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        
        try {
            OneWonVerificationResponse response = accountService.sendOneWonVerification(encryptedUserKey, request.getAccountNo(), userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OneWonVerificationResponse.failure("서버 오류가 발생했습니다."));
        }
    }
    
    @Operation(summary = "1원 송금 인증 코드 검증", description = "1원 송금으로 받은 인증 코드를 검증하고 검증 토큰을 발급합니다")
    @PostMapping("/verification/check")
    public ResponseEntity<OneWonVerificationValidateResponse> validateOneWonAuth(
            @RequestBody OneWonVerificationValidateRequest request) {
        
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        
        try {
            OneWonVerificationValidateResponse response = accountService.validateOneWonAuth(encryptedUserKey, request.getAccountNo(), request.getAuthCode(), userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OneWonVerificationValidateResponse.failure("서버 오류가 발생했습니다."));
        }
    }
    
    @Operation(summary = "주 계좌 등록", description = "검증된 계좌를 주 계좌로 등록합니다")
    @PostMapping("/primary")
    public ResponseEntity<ApiResponse<Void>> registerPrimaryAccount(
            @RequestBody PrimaryAccountRequest request) {
        
        Long userId = JwtUtil.getCurrentUserId();
        
        try {
            ApiResponse<Void> response = accountService.registerPrimaryAccount(
                    request.getAccountNo(), request.getVerificationToken(), userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("서버 오류가 발생했습니다."));
        }
    }
    
    @Operation(summary = "주 계좌 조회", description = "등록된 주 계좌 정보를 조회합니다")
    @GetMapping("/primary")
    public ResponseEntity<ApiResponse<PrimaryAccountInfoDto>> getPrimaryAccount() {
        
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        
        try {
            ApiResponse<PrimaryAccountInfoDto> response = accountService.getPrimaryAccount(encryptedUserKey, userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("서버 오류가 발생했습니다."));
        }
    }
    
    @Operation(summary = "예금주 조회", description = "계좌번호로 예금주 정보를 조회합니다")
    @GetMapping("/holder")
    public ResponseEntity<ApiResponse<AccountHolderInfoDto>> getAccountHolder(
            @RequestParam String accountNo) {
        
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        
        try {
            ApiResponse<AccountHolderInfoDto> response = accountService.getAccountHolder(encryptedUserKey, accountNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("서버 오류가 발생했습니다."));
        }
    }
    
    @Operation(summary = "거래 내역 조회", description = "계좌의 거래 내역을 커서 기반 페이징으로 조회합니다")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<TransactionHistoryResponse>> getTransactionHistory(
            @RequestParam String accountNo,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "DESC") String sort) {
        
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        
        try {
            ApiResponse<TransactionHistoryResponse> response = accountService.getTransactionHistory(encryptedUserKey, accountNo, cursor, size, sort).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("서버 오류가 발생했습니다."));
        }
    }
}