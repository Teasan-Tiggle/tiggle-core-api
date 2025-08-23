package com.example.tiggle.controller.finopenapi;

import com.example.tiggle.dto.finopenapi.response.*;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fintest")
@Tag(name = "Financial API 테스트", description = "SSAFY Financial API 테스트용 컨트롤러")
public class FinancialTestController {
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    public FinancialTestController(FinancialApiService financialApiService, EncryptionService encryptionService) {
        this.financialApiService = financialApiService;
        this.encryptionService = encryptionService;
    }

    @Operation(summary = "사용자 계정 생성", description = "새로운 사용자 계정을 생성합니다")
    @PostMapping("/user")
    public ResponseEntity<UserResponse> createUser(@RequestParam String userId) {
        try {
            UserResponse response = financialApiService.createUser(userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserResponse> searchUser(@PathVariable String userId) {
        try {
            UserResponse response = financialApiService.searchUser(userId).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 생성", description = "새로운 수시입출금 계좌를 생성합니다")
    @PostMapping("/account")
    public ResponseEntity<CreateDemandDepositAccountResponse> createAccount() {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            CreateDemandDepositAccountResponse response = financialApiService.createDemandDepositAccount(userKey).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 목록 조회", description = "사용자의 모든 계좌 목록을 조회합니다")
    @GetMapping("/accounts")
    public ResponseEntity<InquireDemandDepositAccountListResponse> getAccountList() {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireDemandDepositAccountListResponse response = financialApiService.inquireDemandDepositAccountList(userKey).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 잔액 조회", description = "특정 계좌의 현재 잔액을 조회합니다")
    @GetMapping("/account/balance")
    public ResponseEntity<InquireDemandDepositAccountBalanceResponse> getAccountBalance(
            @RequestParam String accountNo) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireDemandDepositAccountBalanceResponse response = financialApiService.inquireDemandDepositAccountBalance(userKey, accountNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 상세 조회", description = "특정 계좌의 상세 정보를 조회합니다")
    @GetMapping("/account/detail")
    public ResponseEntity<InquireDemandDepositAccountResponse> getAccountDetail(
            @RequestParam String accountNo) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireDemandDepositAccountResponse response = financialApiService.inquireDemandDepositAccount(userKey, accountNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 입금", description = "특정 계좌에 금액을 입금합니다")
    @PostMapping("/account/deposit")
    public ResponseEntity<UpdateDemandDepositAccountDepositResponse> deposit(
            @RequestParam String accountNo,
            @RequestParam String transactionBalance,
            @RequestParam String transactionSummary) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            UpdateDemandDepositAccountDepositResponse response = financialApiService.updateDemandDepositAccountDeposit(userKey, accountNo, transactionBalance, transactionSummary).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 출금", description = "특정 계좌에서 금액을 출금합니다")
    @PostMapping("/account/withdraw")
    public ResponseEntity<UpdateDemandDepositAccountWithdrawalResponse> withdraw(
            @RequestParam String accountNo,
            @RequestParam String transactionBalance,
            @RequestParam String transactionSummary) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            UpdateDemandDepositAccountWithdrawalResponse response = financialApiService.updateDemandDepositAccountWithdrawal(userKey, accountNo, transactionBalance, transactionSummary).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 이체", description = "한 계좌에서 다른 계좌로 금액을 이체합니다")
    @PostMapping("/account/transfer")
    public ResponseEntity<UpdateDemandDepositAccountTransferResponse> transfer(
            @RequestParam String depositAccountNo,
            @RequestParam String depositTransactionSummary,
            @RequestParam String transactionBalance,
            @RequestParam String withdrawalAccountNo,
            @RequestParam String withdrawalTransactionSummary) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            UpdateDemandDepositAccountTransferResponse response = financialApiService.updateDemandDepositAccountTransfer(
                    userKey, depositAccountNo, depositTransactionSummary, 
                    transactionBalance, withdrawalAccountNo, withdrawalTransactionSummary).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 거래 내역 조회", description = "특정 계좌의 거래 내역을 조회합니다")
    @GetMapping("/account/transactions")
    public ResponseEntity<InquireTransactionHistoryListResponse> getTransactionHistory(
            @RequestParam String accountNo,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String transactionType,
            @RequestParam String orderByType) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireTransactionHistoryListResponse response = financialApiService.inquireTransactionHistoryList(
                    userKey, accountNo, startDate, endDate, transactionType, orderByType).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 거래 내역 조회(단건)", description = "특정 거래의 상세 정보를 조회합니다")
    @GetMapping("/account/transaction")
    public ResponseEntity<InquireTransactionHistoryResponse> getTransaction(
            @RequestParam String accountNo,
            @RequestParam String transactionUniqueNo) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireTransactionHistoryResponse response = financialApiService.inquireTransactionHistory(userKey, accountNo, transactionUniqueNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "계좌 해지", description = "특정 계좌를 해지합니다")
    @DeleteMapping("/account")
    public ResponseEntity<DeleteDemandDepositAccountResponse> deleteAccount(
            @RequestParam String accountNo) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            DeleteDemandDepositAccountResponse response = financialApiService.deleteDemandDepositAccount(userKey, accountNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "1원 송금(계좌 인증)", description = "계좌 인증을 위한 1원 송금을 실행합니다")
    @PostMapping("/account/auth")
    public ResponseEntity<OpenAccountAuthResponse> openAccountAuth(
            @RequestParam String accountNo,
            @RequestParam String authText) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            OpenAccountAuthResponse response = financialApiService.openAccountAuth(userKey, accountNo, authText).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "1원 송금 인증 검증", description = "1원 송금 인증 코드를 검증합니다")
    @PostMapping("/account/auth/verify")
    public ResponseEntity<CheckAuthCodeResponse> checkAuthCode(
            @RequestParam String accountNo,
            @RequestParam String authText,
            @RequestParam String authCode) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            CheckAuthCodeResponse response = financialApiService.checkAuthCode(userKey, accountNo, authText, authCode).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(summary = "계좌 소유자명 조회", description = "계좌의 소유자명을 조회합니다")
    @GetMapping("/account/holder")
    public ResponseEntity<InquireDemandDepositAccountHolderNameResponse> getAccountHolderName(
            @RequestParam String accountNo) {
        try {
            String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
            String userKey = encryptionService.decrypt(encryptedUserKey);
            InquireDemandDepositAccountHolderNameResponse response = financialApiService.inquireDemandDepositAccountHolderName(userKey, accountNo).block();
            return ResponseEntity.ok(response);
        } catch (Exception error) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
