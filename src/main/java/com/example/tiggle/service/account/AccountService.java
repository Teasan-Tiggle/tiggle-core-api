package com.example.tiggle.service.account;

import com.example.tiggle.dto.account.response.AccountHolderInfoDto;
import com.example.tiggle.dto.account.response.OneWonVerificationResponse;
import com.example.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.account.response.TransactionHistoryResponse;
import com.example.tiggle.dto.common.ApiResponse;
import reactor.core.publisher.Mono;

public interface AccountService {
    
    Mono<OneWonVerificationResponse> sendOneWonVerification(String encryptedUserKey, String accountNo, Long userId);
    
    Mono<OneWonVerificationValidateResponse> validateOneWonAuth(String encryptedUserKey, String accountNo, String authCode, Long userId);
    
    Mono<ApiResponse<Void>> registerPrimaryAccount(String accountNo, String verificationToken, Long userId);
    
    Mono<ApiResponse<PrimaryAccountInfoDto>> getPrimaryAccount(String encryptedUserKey, Long userId);
    
    Mono<ApiResponse<AccountHolderInfoDto>> getAccountHolder(String encryptedUserKey, String accountNo);
    
    Mono<ApiResponse<TransactionHistoryResponse>> getTransactionHistory(String encryptedUserKey, String accountNo, String cursor, Integer size, String sort);

    Mono<Void> transferTiggleToPiggy(String encryptedUserKey, Long userId, Long dutchpayId, long tiggleAmount);

    Mono<Void> transferTiggleIfPayMore(String encryptedUserKey, Long userId, Long dutchpayId, long originalAmount, boolean payMoreSelected);
}