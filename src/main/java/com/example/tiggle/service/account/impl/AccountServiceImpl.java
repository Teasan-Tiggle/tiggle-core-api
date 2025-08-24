package com.example.tiggle.service.account.impl;

import com.example.tiggle.dto.account.response.AccountHolderInfoDto;
import com.example.tiggle.dto.account.response.OneWonVerificationResponse;
import com.example.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.account.response.TransactionDto;
import com.example.tiggle.dto.account.response.TransactionHistoryResponse;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.account.AccountService;
import com.example.tiggle.service.account.AccountVerificationTokenService;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    
    private final FinancialApiService financialApiService;
    private final AccountVerificationTokenService tokenService;
    private final StudentRepository studentRepository;
    private final EncryptionService encryptionService;
    
    @Override
    public Mono<OneWonVerificationResponse> sendOneWonVerification(String encryptedUserKey, String accountNo) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        
        return financialApiService.openAccountAuth(userKey, accountNo, "티끌")
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        return OneWonVerificationResponse.success();
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "알 수 없는 오류가 발생했습니다.";
                        return OneWonVerificationResponse.failure(errorMessage);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("1원 송금 API 호출 중 오류 발생", throwable);
                    return Mono.just(OneWonVerificationResponse.failure("계좌 인증 중 오류가 발생했습니다."));
                });
    }
    
    @Override
    public Mono<OneWonVerificationValidateResponse> validateOneWonAuth(String encryptedUserKey, String accountNo, String authCode, Long userId) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        return financialApiService.checkAuthCode(userKey, accountNo, "티끌", authCode)
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        Users users = studentRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
                        String verificationToken = tokenService.generateVerificationToken(accountNo, users);
                        return OneWonVerificationValidateResponse.success(verificationToken);
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "알 수 없는 오류가 발생했습니다.";
                        return OneWonVerificationValidateResponse.failure(errorMessage);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("1원 송금 인증 코드 검증 API 호출 중 오류 발생", throwable);
                    return Mono.just(OneWonVerificationValidateResponse.failure("인증 코드 검증 중 오류가 발생했습니다."));
                });
    }
    
    @Override
    public Mono<ApiResponse<Void>> registerPrimaryAccount(String accountNo, String verificationToken, Long userId) {
        return Mono.fromCallable(() -> {
            if (!tokenService.validateTokenForAccount(verificationToken, accountNo)) {
                return ApiResponse.<Void>failure("유효하지 않은 검증 토큰이거나 계좌번호가 일치하지 않습니다.");
            }
            
            Users user = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            user.setPrimaryAccountNo(accountNo);
            studentRepository.save(user);
            
            tokenService.markTokenAsUsed(verificationToken);
            
            log.info("주 계좌 등록 완료 - 사용자ID: {}, 계좌번호: {}", user.getId(), accountNo);
            return ApiResponse.success();
        });
    }
    
    @Override
    public Mono<ApiResponse<PrimaryAccountInfoDto>> getPrimaryAccount(String encryptedUserKey, Long userId) {
        return Mono.fromCallable(() -> {
            Users user = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            if (user.getPrimaryAccountNo() == null) {
                throw new RuntimeException("등록된 주 계좌가 없습니다.");
            }
            
            return user.getPrimaryAccountNo();
        })
        .flatMap(accountNo -> {
            String userKey = encryptionService.decrypt(encryptedUserKey);
            return financialApiService.inquireDemandDepositAccount(userKey, accountNo)
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        PrimaryAccountInfoDto accountInfo = new PrimaryAccountInfoDto(
                                response.getRec().getAccountName(),
                                response.getRec().getAccountNo(),
                                response.getRec().getAccountBalance()
                        );
                        return ApiResponse.success(accountInfo);
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "계좌 조회 중 오류가 발생했습니다.";
                        return ApiResponse.<PrimaryAccountInfoDto>failure(errorMessage);
                    }
                });
        })
        .onErrorResume(throwable -> {
            log.error("주 계좌 조회 중 오류 발생", throwable);
            String errorMessage = "등록된 주 계좌가 없습니다.".equals(throwable.getMessage()) 
                    ? throwable.getMessage() 
                    : "계좌 조회 중 오류가 발생했습니다.";
            return Mono.just(ApiResponse.failure(errorMessage));
        });
    }
    
    @Override
    public Mono<ApiResponse<AccountHolderInfoDto>> getAccountHolder(String encryptedUserKey, String accountNo) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        
        return financialApiService.inquireDemandDepositAccountHolderName(userKey, accountNo)
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        AccountHolderInfoDto holderInfo = new AccountHolderInfoDto(
                                response.getRec().getBankName(),
                                response.getRec().getAccountNo(),
                                response.getRec().getUserName()
                        );
                        return ApiResponse.success(holderInfo);
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "예금주 조회 중 오류가 발생했습니다.";
                        return ApiResponse.<AccountHolderInfoDto>failure(errorMessage);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("예금주 조회 중 오류 발생", throwable);
                    return Mono.just(ApiResponse.failure("예금주 조회 중 오류가 발생했습니다."));
                });
    }
    
    @Override
    public Mono<ApiResponse<TransactionHistoryResponse>> getTransactionHistory(String encryptedUserKey, String accountNo, String cursor, Integer size, String sort) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        
        // 날짜 범위 설정 (최근 3개월)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);
        
        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        return financialApiService.inquireTransactionHistoryList(
                userKey, accountNo, startDateStr, endDateStr, "A", sort)
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        List<TransactionDto> allTransactions = response.getRec().getList().stream()
                                .map(this::convertToTransactionDto)
                                .collect(Collectors.toList());
                        
                        // Cursor 기반 필터링 및 페이징
                        List<TransactionDto> filteredTransactions = applyCursorAndPaging(allTransactions, cursor, size, sort);
                        
                        // 다음 커서 계산
                        String nextCursor = null;
                        boolean hasNext = false;
                        
                        if (!filteredTransactions.isEmpty()) {
                            TransactionDto lastTransaction = filteredTransactions.get(filteredTransactions.size() - 1);
                            nextCursor = lastTransaction.getTransactionId();
                            
                            // 다음 페이지가 있는지 확인
                            hasNext = checkHasNext(allTransactions, nextCursor, sort);
                        }
                        
                        TransactionHistoryResponse historyResponse = new TransactionHistoryResponse(
                                filteredTransactions, nextCursor, hasNext, filteredTransactions.size());
                        
                        return ApiResponse.success(historyResponse);
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "거래 내역 조회 중 오류가 발생했습니다.";
                        return ApiResponse.<TransactionHistoryResponse>failure(errorMessage);
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("거래 내역 조회 중 오류 발생", throwable);
                    return Mono.just(ApiResponse.failure("거래 내역 조회 중 오류가 발생했습니다."));
                });
    }
    
    private TransactionDto convertToTransactionDto(com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC rec) {
        // 날짜 포맷 변환 (20250822 -> 2025-08-22)
        String formattedDate = rec.getTransactionDate().substring(0, 4) + "-" + 
                              rec.getTransactionDate().substring(4, 6) + "-" + 
                              rec.getTransactionDate().substring(6, 8);
        
        // 시간 포맷 변환 (142919 -> 14:29:19)
        String formattedTime = rec.getTransactionTime().substring(0, 2) + ":" + 
                              rec.getTransactionTime().substring(2, 4) + ":" + 
                              rec.getTransactionTime().substring(4, 6);
        
        return new TransactionDto(
                rec.getTransactionUniqueNo(),
                formattedDate,
                formattedTime,
                rec.getTransactionTypeName(),
                rec.getTransactionSummary(),
                rec.getTransactionBalance(),
                rec.getTransactionAfterBalance()
        );
    }
    
    private List<TransactionDto> applyCursorAndPaging(List<TransactionDto> transactions, String cursor, Integer size, String sort) {
        List<TransactionDto> filtered = transactions;
        
        // Cursor 필터링
        if (cursor != null && !cursor.isEmpty()) {
            if ("DESC".equals(sort)) {
                // 최신순: cursor보다 작은 값들
                filtered = transactions.stream()
                        .filter(t -> Long.parseLong(t.getTransactionId()) < Long.parseLong(cursor))
                        .collect(Collectors.toList());
            } else {
                // 과거순: cursor보다 큰 값들  
                filtered = transactions.stream()
                        .filter(t -> Long.parseLong(t.getTransactionId()) > Long.parseLong(cursor))
                        .collect(Collectors.toList());
            }
        }
        
        // 페이징
        return filtered.stream()
                .limit(size != null ? size : 20)
                .collect(Collectors.toList());
    }
    
    private boolean checkHasNext(List<TransactionDto> allTransactions, String cursor, String sort) {
        if ("DESC".equals(sort)) {
            return allTransactions.stream()
                    .anyMatch(t -> Long.parseLong(t.getTransactionId()) < Long.parseLong(cursor));
        } else {
            return allTransactions.stream()
                    .anyMatch(t -> Long.parseLong(t.getTransactionId()) > Long.parseLong(cursor));
        }
    }
}