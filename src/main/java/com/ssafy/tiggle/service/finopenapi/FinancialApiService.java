package com.ssafy.tiggle.service.finopenapi;

import com.ssafy.tiggle.dto.finopenapi.response.*;
import reactor.core.publisher.Mono;

public interface FinancialApiService {
    
    // 사용자 계정 생성
    Mono<UserResponse> createUser(String userId);
    
    // 사용자 조회
    Mono<UserResponse> searchUser(String userId);
    
    // 계좌 생성
    Mono<CreateDemandDepositAccountResponse> createDemandDepositAccount(String userKey);
    
    // 계좌 목록 조회
    Mono<InquireDemandDepositAccountListResponse> inquireDemandDepositAccountList(String userKey);

    // 계좌 조회(단건)
    Mono<InquireDemandDepositAccountResponse> inquireDemandDepositAccount(String userKey, String accountNo);
    
    // 계좌 잔액 조회
    Mono<InquireDemandDepositAccountBalanceResponse> inquireDemandDepositAccountBalance(String userKey, String accountNo);
    
    // 계좌 입금
    Mono<UpdateDemandDepositAccountDepositResponse> updateDemandDepositAccountDeposit(String userKey, String accountNo, String transactionBalance, String transactionSummary);
    
    // 계좌 출금
    Mono<UpdateDemandDepositAccountWithdrawalResponse> updateDemandDepositAccountWithdrawal(String userKey, String accountNo, String transactionBalance, String transactionSummary);
    
    // 계좌 이체
    Mono<UpdateDemandDepositAccountTransferResponse> updateDemandDepositAccountTransfer(String userKey, String depositAccountNo, String depositTransactionSummary, String transactionBalance, String withdrawalAccountNo, String withdrawalTransactionSummary);
    
    // 계좌 거래 내역 조회
    Mono<InquireTransactionHistoryListResponse> inquireTransactionHistoryList(String userKey, String accountNo, String startDate, String endDate, String transactionType, String orderByType);
    
    // 계좌 거래 내역 조회(단건)
    Mono<InquireTransactionHistoryResponse> inquireTransactionHistory(String userKey, String accountNo, String transactionUniqueNo);
    
    // 계좌 해지
    Mono<DeleteDemandDepositAccountResponse> deleteDemandDepositAccount(String userKey, String accountNo);
    
    // 1원 송금(계좌 인증)
    Mono<OpenAccountAuthResponse> openAccountAuth(String userKey, String accountNo, String authText);
    
    // 1원 송금 인증 검증
    Mono<CheckAuthCodeResponse> checkAuthCode(String userKey, String accountNo, String authText, String authCode);

    // 예금주 조회
    Mono<InquireDemandDepositAccountHolderNameResponse> inquireDemandDepositAccountHolderName(String userKey, String accountNo);
}