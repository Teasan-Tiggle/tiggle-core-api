package com.example.tiggle.service.account.impl;

import com.example.tiggle.dto.account.response.OneWonVerificationResponse;
import com.example.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.entity.Student;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.account.AccountService;
import com.example.tiggle.service.account.AccountVerificationTokenService;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    
    private final FinancialApiService financialApiService;
    private final AccountVerificationTokenService tokenService;
    private final StudentRepository studentRepository;
    
    @Override
    public Mono<OneWonVerificationResponse> sendOneWonVerification(String encryptedUserKey, String accountNo) {
        return financialApiService.openAccountAuth(encryptedUserKey, accountNo, "계좌인증")
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
    public Mono<OneWonVerificationValidateResponse> validateOneWonAuth(String encryptedUserKey, String accountNo, String authCode, Integer userId) {
        return financialApiService.checkAuthCode(encryptedUserKey, accountNo, "계좌인증", authCode)
                .map(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        Student student = studentRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
                        String verificationToken = tokenService.generateVerificationToken(accountNo, student);
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
    public Mono<ApiResponse<Void>> registerPrimaryAccount(String accountNo, String verificationToken, Integer userId) {
        return Mono.fromCallable(() -> {
            if (!tokenService.validateToken(verificationToken)) {
                return ApiResponse.<Void>failure("유효하지 않은 검증 토큰입니다.");
            }
            
            Student student = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            student.setPrimaryAccountNo(accountNo);
            studentRepository.save(student);
            
            tokenService.markTokenAsUsed(verificationToken);
            
            log.info("주 계좌 등록 완료 - 사용자ID: {}, 계좌번호: {}", student.getId(), accountNo);
            return ApiResponse.<Void>success();
        });
    }
    
    @Override
    public Mono<ApiResponse<PrimaryAccountInfoDto>> getPrimaryAccount(String encryptedUserKey, Integer userId) {
        return Mono.fromCallable(() -> {
            Student student = studentRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            if (student.getPrimaryAccountNo() == null) {
                throw new RuntimeException("등록된 주 계좌가 없습니다.");
            }
            
            return student.getPrimaryAccountNo();
        })
        .flatMap(accountNo -> 
            financialApiService.inquireDemandDepositAccount(encryptedUserKey, accountNo)
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
                })
        )
        .onErrorResume(throwable -> {
            log.error("주 계좌 조회 중 오류 발생", throwable);
            String errorMessage = "등록된 주 계좌가 없습니다.".equals(throwable.getMessage()) 
                    ? throwable.getMessage() 
                    : "계좌 조회 중 오류가 발생했습니다.";
            return Mono.just(ApiResponse.<PrimaryAccountInfoDto>failure(errorMessage));
        });
    }
}