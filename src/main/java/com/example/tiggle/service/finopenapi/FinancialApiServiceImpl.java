package com.example.tiggle.service.finopenapi;

import com.example.tiggle.constants.FinancialApiEndpoints;
import com.example.tiggle.dto.finopenapi.request.*;
import com.example.tiggle.dto.finopenapi.response.CheckAuthCodeResponse;
import com.example.tiggle.dto.finopenapi.response.CreateDemandDepositAccountResponse;
import com.example.tiggle.dto.finopenapi.response.UserResponse;
import com.example.tiggle.dto.finopenapi.response.DeleteDemandDepositAccountResponse;
import com.example.tiggle.dto.finopenapi.response.InquireDemandDepositAccountBalanceResponse;
import com.example.tiggle.dto.finopenapi.response.InquireDemandDepositAccountHolderNameResponse;
import com.example.tiggle.dto.finopenapi.response.InquireDemandDepositAccountListResponse;
import com.example.tiggle.dto.finopenapi.response.InquireDemandDepositAccountResponse;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListResponse;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryResponse;
import com.example.tiggle.dto.finopenapi.response.OpenAccountAuthResponse;
import com.example.tiggle.dto.finopenapi.response.UpdateDemandDepositAccountDepositResponse;
import com.example.tiggle.dto.finopenapi.response.UpdateDemandDepositAccountTransferResponse;
import com.example.tiggle.dto.finopenapi.response.UpdateDemandDepositAccountWithdrawalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.example.tiggle.util.FinancialApiHeaderUtil.createHeader;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialApiServiceImpl implements FinancialApiService {

    private final WebClient ssafyFinApiWebClient;

    @Value("${external-api.ssafy.api-key}")
    private String apiKey;

    // 사용자 계정 생성
    @Override
    public Mono<UserResponse> createUser(String userId) {
        UserRequest request = UserRequest.builder()
                .userId(userId)
                .apiKey(apiKey)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.MEMBER_BASE + FinancialApiEndpoints.CREATE_USER)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    // 사용자 조회
    @Override
    public Mono<UserResponse> searchUser(String userId) {
        UserRequest request = UserRequest.builder()
                .userId(userId)
                .apiKey(apiKey)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.MEMBER_BASE + FinancialApiEndpoints.SEARCH_USER)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserResponse.class);
    }

    // 계좌 생성
    @Override
    public Mono<CreateDemandDepositAccountResponse> createDemandDepositAccount(String userKey) {
        CreateDemandDepositAccountRequest request = CreateDemandDepositAccountRequest.builder()
                .header(createHeader("createDemandDepositAccount", apiKey, userKey))
                .accountTypeUniqueNo("088-1-c099deaaef7c41")
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.CREATE_DEMAND_DEPOSIT_ACCOUNT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreateDemandDepositAccountResponse.class);
    }

    // 계좌 목록 조회
    @Override
    public Mono<InquireDemandDepositAccountListResponse> inquireDemandDepositAccountList(String userKey) {
        InquireDemandDepositAccountListRequest request = InquireDemandDepositAccountListRequest.builder()
                .header(createHeader("inquireDemandDepositAccountList", apiKey, userKey))
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_DEMAND_DEPOSIT_ACCOUNT_LIST)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireDemandDepositAccountListResponse.class);
    }

    // 계좌 조회(단건)
    @Override
    public Mono<InquireDemandDepositAccountResponse> inquireDemandDepositAccount(String userKey, String accountNo) {
        InquireDemandDepositAccountRequest request = InquireDemandDepositAccountRequest.builder()
                .header(createHeader("inquireDemandDepositAccount", apiKey, userKey))
                .accountNo(accountNo)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_DEMAND_DEPOSIT_ACCOUNT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireDemandDepositAccountResponse.class);
    }

    // 계좌 잔액 조회
    @Override
    public Mono<InquireDemandDepositAccountBalanceResponse> inquireDemandDepositAccountBalance(String userKey, String accountNo) {
        InquireDemandDepositAccountBalanceRequest request = InquireDemandDepositAccountBalanceRequest.builder()
                .header(createHeader("inquireDemandDepositAccountBalance", apiKey, userKey))
                .accountNo(accountNo)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_DEMAND_DEPOSIT_ACCOUNT_BALANCE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireDemandDepositAccountBalanceResponse.class);
    }

    // 계좌 입금
    @Override
    public Mono<UpdateDemandDepositAccountDepositResponse> updateDemandDepositAccountDeposit(String userKey, String accountNo, String transactionBalance, String transactionSummary) {
        UpdateDemandDepositAccountDepositRequest request = UpdateDemandDepositAccountDepositRequest.builder()
                .header(createHeader("updateDemandDepositAccountDeposit", apiKey, userKey))
                .accountNo(accountNo)
                .transactionBalance(transactionBalance)
                .transactionSummary(transactionSummary)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.UPDATE_DEMAND_DEPOSIT_ACCOUNT_DEPOSIT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UpdateDemandDepositAccountDepositResponse.class);
    }

    // 계좌 출금
    @Override
    public Mono<UpdateDemandDepositAccountWithdrawalResponse> updateDemandDepositAccountWithdrawal(String userKey, String accountNo, String transactionBalance, String transactionSummary) {
        UpdateDemandDepositAccountWithdrawalRequest request = UpdateDemandDepositAccountWithdrawalRequest.builder()
                .header(createHeader("updateDemandDepositAccountWithdrawal", apiKey, userKey))
                .accountNo(accountNo)
                .transactionBalance(transactionBalance)
                .transactionSummary(transactionSummary)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.UPDATE_DEMAND_DEPOSIT_ACCOUNT_WITHDRAWAL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UpdateDemandDepositAccountWithdrawalResponse.class);
    }

    // 계좌 이체
    @Override
    public Mono<UpdateDemandDepositAccountTransferResponse> updateDemandDepositAccountTransfer(String userKey, String depositAccountNo, String depositTransactionSummary, String transactionBalance, String withdrawalAccountNo, String withdrawalTransactionSummary) {
        UpdateDemandDepositAccountTransferRequest request = UpdateDemandDepositAccountTransferRequest.builder()
                .header(createHeader("updateDemandDepositAccountTransfer", apiKey, userKey))
                .depositAccountNo(depositAccountNo)
                .depositTransactionSummary(depositTransactionSummary)
                .transactionBalance(transactionBalance)
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalTransactionSummary(withdrawalTransactionSummary)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.UPDATE_DEMAND_DEPOSIT_ACCOUNT_TRANSFER)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UpdateDemandDepositAccountTransferResponse.class);
    }

    // 계좌 거래 내역 조회
    @Override
    public Mono<InquireTransactionHistoryListResponse> inquireTransactionHistoryList(String userKey, String accountNo, String startDate, String endDate, String transactionType, String orderByType) {
        InquireTransactionHistoryListRequest request = InquireTransactionHistoryListRequest.builder()
                .header(createHeader("inquireTransactionHistoryList", apiKey, userKey))
                .accountNo(accountNo)
                .startDate(startDate)
                .endDate(endDate)
                .transactionType(transactionType)
                .orderByType(orderByType)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_TRANSACTION_HISTORY_LIST)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireTransactionHistoryListResponse.class);
    }

    // 계좌 거래 내역 조회(단건)
    @Override
    public Mono<InquireTransactionHistoryResponse> inquireTransactionHistory(String userKey, String accountNo, String transactionUniqueNo) {
        InquireTransactionHistoryRequest request = InquireTransactionHistoryRequest.builder()
                .header(createHeader("inquireTransactionHistory", apiKey, userKey))
                .accountNo(accountNo)
                .transactionUniqueNo(transactionUniqueNo)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_TRANSACTION_HISTORY)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireTransactionHistoryResponse.class);
    }

    // 계좌 해지
    @Override
    public Mono<DeleteDemandDepositAccountResponse> deleteDemandDepositAccount(String userKey, String accountNo) {
        DeleteDemandDepositAccountRequest request = DeleteDemandDepositAccountRequest.builder()
                .header(createHeader("deleteDemandDepositAccount", apiKey, userKey))
                .accountNo(accountNo)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.DELETE_DEMAND_DEPOSIT_ACCOUNT)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeleteDemandDepositAccountResponse.class);
    }

    // 1원 송금(계좌 인증)
    @Override
    public Mono<OpenAccountAuthResponse> openAccountAuth(String userKey, String accountNo, String authText) {
        OpenAccountAuthRequest request = OpenAccountAuthRequest.builder()
                .header(createHeader("openAccountAuth", apiKey, userKey))
                .accountNo(accountNo)
                .authText(authText)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.ACCOUNT_AUTH_BASE + FinancialApiEndpoints.OPEN_ACCOUNT_AUTH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAccountAuthResponse.class);
    }

    // 1원 송금 인증 검증
    @Override
    public Mono<CheckAuthCodeResponse> checkAuthCode(String userKey, String accountNo, String authText, String authCode) {
        CheckAuthCodeRequest request = CheckAuthCodeRequest.builder()
                .header(createHeader("checkAuthCode", apiKey, userKey))
                .accountNo(accountNo)
                .authText(authText)
                .authCode(authCode)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.ACCOUNT_AUTH_BASE + FinancialApiEndpoints.CHECK_AUTH_CODE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CheckAuthCodeResponse.class);
    }

    // 예금주 조회
    @Override
    public Mono<InquireDemandDepositAccountHolderNameResponse> inquireDemandDepositAccountHolderName(String userKey, String accountNo) {
        InquireDemandDepositAccountHolderNameRequest request = InquireDemandDepositAccountHolderNameRequest.builder()
                .header(createHeader("inquireDemandDepositAccountHolderName", apiKey, userKey))
                .accountNo(accountNo)
                .build();

        return ssafyFinApiWebClient
                .post()
                .uri(FinancialApiEndpoints.DEMAND_DEPOSIT_BASE + FinancialApiEndpoints.INQUIRE_DEMAND_DEPOSIT_ACCOUNT_HOLDER_NAME)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InquireDemandDepositAccountHolderNameResponse.class);
    }
}