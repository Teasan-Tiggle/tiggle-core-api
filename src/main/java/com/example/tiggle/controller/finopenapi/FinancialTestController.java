package com.example.tiggle.controller.finopenapi;

import com.example.tiggle.dto.finopenapi.response.*;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/fintest")
@Tag(name = "Financial API 테스트", description = "SSAFY Financial API 테스트용 컨트롤러")
public class FinancialTestController {
    private final FinancialApiService financialApiService;

    public FinancialTestController(FinancialApiService financialApiService) {
        this.financialApiService = financialApiService;
    }

    @Operation(summary = "사용자 계정 생성", description = "새로운 사용자 계정을 생성합니다")
    @PostMapping("/user")
    public Mono<UserResponse> createUser(@RequestParam String userId) {
        return financialApiService.createUser(userId);
    }

    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다")
    @GetMapping("/user/{userId}")
    public Mono<UserResponse> searchUser(@PathVariable String userId) {
        return financialApiService.searchUser(userId);
    }

    @Operation(summary = "계좌 생성", description = "새로운 수시입출금 계좌를 생성합니다")
    @PostMapping("/account")
    public Mono<CreateDemandDepositAccountResponse> createAccount(@RequestParam String userKey) {
        return financialApiService.createDemandDepositAccount(userKey);
    }

    @Operation(summary = "계좌 목록 조회", description = "사용자의 모든 계좌 목록을 조회합니다")
    @GetMapping("/accounts")
    public Mono<InquireDemandDepositAccountListResponse> getAccountList(@RequestParam String userKey) {
        return financialApiService.inquireDemandDepositAccountList(userKey);
    }

    @Operation(summary = "계좌 잔액 조회", description = "특정 계좌의 현재 잔액을 조회합니다")
    @GetMapping("/account/balance")
    public Mono<InquireDemandDepositAccountBalanceResponse> getAccountBalance(
            @RequestParam String userKey, 
            @RequestParam String accountNo) {
        return financialApiService.inquireDemandDepositAccountBalance(userKey, accountNo);
    }

    @Operation(summary = "계좌 상세 조회", description = "특정 계좌의 상세 정보를 조회합니다")
    @GetMapping("/account/detail")
    public Mono<InquireDemandDepositAccountResponse> getAccountDetail(
            @RequestParam String userKey, 
            @RequestParam String accountNo) {
        return financialApiService.inquireDemandDepositAccount(userKey, accountNo);
    }

    @Operation(summary = "계좌 입금", description = "특정 계좌에 금액을 입금합니다")
    @PostMapping("/account/deposit")
    public Mono<UpdateDemandDepositAccountDepositResponse> deposit(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String transactionBalance,
            @RequestParam String transactionSummary) {
        return financialApiService.updateDemandDepositAccountDeposit(userKey, accountNo, transactionBalance, transactionSummary);
    }

    @Operation(summary = "계좌 출금", description = "특정 계좌에서 금액을 출금합니다")
    @PostMapping("/account/withdraw")
    public Mono<UpdateDemandDepositAccountWithdrawalResponse> withdraw(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String transactionBalance,
            @RequestParam String transactionSummary) {
        return financialApiService.updateDemandDepositAccountWithdrawal(userKey, accountNo, transactionBalance, transactionSummary);
    }

    @Operation(summary = "계좌 이체", description = "한 계좌에서 다른 계좌로 금액을 이체합니다")
    @PostMapping("/account/transfer")
    public Mono<UpdateDemandDepositAccountTransferResponse> transfer(
            @RequestParam String userKey,
            @RequestParam String depositAccountNo,
            @RequestParam String depositTransactionSummary,
            @RequestParam String transactionBalance,
            @RequestParam String withdrawalAccountNo,
            @RequestParam String withdrawalTransactionSummary) {
        return financialApiService.updateDemandDepositAccountTransfer(
                userKey, depositAccountNo, depositTransactionSummary, 
                transactionBalance, withdrawalAccountNo, withdrawalTransactionSummary);
    }

    @Operation(summary = "계좌 거래 내역 조회", description = "특정 계좌의 거래 내역을 조회합니다")
    @GetMapping("/account/transactions")
    public Mono<InquireTransactionHistoryListResponse> getTransactionHistory(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String transactionType,
            @RequestParam String orderByType) {
        return financialApiService.inquireTransactionHistoryList(
                userKey, accountNo, startDate, endDate, transactionType, orderByType);
    }

    @Operation(summary = "계좌 거래 내역 조회(단건)", description = "특정 거래의 상세 정보를 조회합니다")
    @GetMapping("/account/transaction")
    public Mono<InquireTransactionHistoryResponse> getTransaction(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String transactionUniqueNo) {
        return financialApiService.inquireTransactionHistory(userKey, accountNo, transactionUniqueNo);
    }

    @Operation(summary = "계좌 해지", description = "특정 계좌를 해지합니다")
    @DeleteMapping("/account")
    public Mono<DeleteDemandDepositAccountResponse> deleteAccount(
            @RequestParam String userKey,
            @RequestParam String accountNo) {
        return financialApiService.deleteDemandDepositAccount(userKey, accountNo);
    }

    @Operation(summary = "1원 송금(계좌 인증)", description = "계좌 인증을 위한 1원 송금을 실행합니다")
    @PostMapping("/account/auth")
    public Mono<OpenAccountAuthResponse> openAccountAuth(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String authText) {
        return financialApiService.openAccountAuth(userKey, accountNo, authText);
    }

    @Operation(summary = "1원 송금 인증 검증", description = "1원 송금 인증 코드를 검증합니다")
    @PostMapping("/account/auth/verify")
    public Mono<CheckAuthCodeResponse> checkAuthCode(
            @RequestParam String userKey,
            @RequestParam String accountNo,
            @RequestParam String authText,
            @RequestParam String authCode) {
        return financialApiService.checkAuthCode(userKey, accountNo, authText, authCode);
    }
}
