package com.ssafy.tiggle.service.account.impl;

import com.ssafy.tiggle.dto.account.response.AccountHolderInfoDto;
import com.ssafy.tiggle.dto.account.response.OneWonVerificationResponse;
import com.ssafy.tiggle.dto.account.response.OneWonVerificationValidateResponse;
import com.ssafy.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.ssafy.tiggle.dto.account.response.TransactionDto;
import com.ssafy.tiggle.dto.account.response.TransactionHistoryResponse;
import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.entity.Dutchpay;
import com.ssafy.tiggle.entity.DutchpayShare;
import com.ssafy.tiggle.entity.Users;
import com.ssafy.tiggle.repository.dutchpay.DutchpayRepository;
import com.ssafy.tiggle.repository.dutchpay.DutchpayShareRepository;
import com.ssafy.tiggle.repository.piggybank.PiggyBankRepository;
import com.ssafy.tiggle.exception.account.AccountException;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.ssafy.tiggle.service.account.AccountService;
import com.ssafy.tiggle.service.account.AccountVerificationTokenService;
import com.ssafy.tiggle.service.finopenapi.FinancialApiService;
import com.ssafy.tiggle.service.notification.FcmService;
import com.ssafy.tiggle.service.piggybank.PiggyBankWriterService;
import com.ssafy.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import com.ssafy.tiggle.entity.DutchpayShareStatus;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    
    private final FinancialApiService financialApiService;
    private final AccountVerificationTokenService tokenService;
    private final StudentRepository studentRepository;
    private final EncryptionService encryptionService;
    private final FcmService fcmService;
    private final PiggyBankRepository piggyBankRepository;
    private final PiggyBankWriterService piggyBankWriterService;
    private final DutchpayRepository dutchpayRepo;
    private final DutchpayShareRepository shareRepo;

    private static record LinkedAccounts(String userKey, String primaryAccountNo, String piggyAccountNo) {}

    @Override
    public Mono<OneWonVerificationResponse> sendOneWonVerification(String encryptedUserKey, String accountNo, Long userId) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        
        return financialApiService.openAccountAuth(userKey, accountNo, "티끌")
                .flatMap(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        getAuthCodeAndSendNotification(encryptedUserKey, accountNo, userId);
                        return Mono.just(OneWonVerificationResponse.success());
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "알 수 없는 오류가 발생했습니다.";
                        return Mono.error(AccountException.verificationFailed(errorMessage));
                    }
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof AccountException) {
                        return Mono.error(throwable);
                    }
                    log.error("1원 송금 API 호출 중 오류 발생", throwable);
                    return Mono.error(AccountException.bankApiError("계좌 인증 중 오류가 발생했습니다."));
                });
    }
    
    @Override
    public Mono<OneWonVerificationValidateResponse> validateOneWonAuth(String encryptedUserKey, String accountNo, String authCode, Long userId) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        return financialApiService.checkAuthCode(userKey, accountNo, "티끌", authCode)
                .flatMap(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        Users users = studentRepository.findById(userId)
                                .orElseThrow(AccountException::userNotFound);
                        String verificationToken = tokenService.generateVerificationToken(accountNo, users);
                        return Mono.just(OneWonVerificationValidateResponse.success(verificationToken));
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "인증 코드가 올바르지 않습니다.";
                        return Mono.error(AccountException.verificationFailed(errorMessage));
                    }
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof AccountException) {
                        return Mono.error(throwable);
                    }
                    log.error("1원 송금 인증 코드 검증 API 호출 중 오류 발생", throwable);
                    return Mono.error(AccountException.bankApiError("인증 코드 검증 중 오류가 발생했습니다."));
                });
    }
    
    @Override
    public Mono<ApiResponse<Void>> registerPrimaryAccount(String accountNo, String verificationToken, Long userId) {
        return Mono.fromCallable(() -> {
            if (!tokenService.validateTokenForAccount(verificationToken, accountNo)) {
                throw AccountException.invalidVerificationToken();
            }
            
            Users user = studentRepository.findById(userId)
                    .orElseThrow(AccountException::userNotFound);
            
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
                    .orElseThrow(AccountException::userNotFound);
            
            if (user.getPrimaryAccountNo() == null) {
                throw AccountException.primaryAccountNotFound();
            }
            
            return user.getPrimaryAccountNo();
        })
        .flatMap(accountNo -> {
            String userKey = encryptionService.decrypt(encryptedUserKey);
            return financialApiService.inquireDemandDepositAccount(userKey, accountNo)
                .flatMap(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        PrimaryAccountInfoDto accountInfo = new PrimaryAccountInfoDto(
                                response.getRec().getAccountName(),
                                response.getRec().getAccountNo(),
                                response.getRec().getAccountBalance()
                        );
                        return Mono.just(ApiResponse.success(accountInfo));
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "계좌 조회 중 오류가 발생했습니다.";
                        return Mono.error(AccountException.bankApiError(errorMessage));
                    }
                });
        })
        .onErrorResume(throwable -> {
            if (throwable instanceof AccountException) {
                return Mono.error(throwable);
            }
            log.error("주 계좌 조회 중 오류 발생", throwable);
            return Mono.error(AccountException.bankApiError("계좌 조회 중 오류가 발생했습니다."));
        });
    }
    
    @Override
    public Mono<ApiResponse<AccountHolderInfoDto>> getAccountHolder(String encryptedUserKey, String accountNo) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        
        return financialApiService.inquireDemandDepositAccountHolderName(userKey, accountNo)
                .flatMap(response -> {
                    if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                        AccountHolderInfoDto holderInfo = new AccountHolderInfoDto(
                                response.getRec().getBankName(),
                                response.getRec().getAccountNo(),
                                response.getRec().getUserName()
                        );
                        return Mono.just(ApiResponse.success(holderInfo));
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "예금주를 찾을 수 없습니다.";
                        return Mono.error(AccountException.accountNotFound());
                    }
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof AccountException) {
                        return Mono.error(throwable);
                    }
                    log.error("예금주 조회 중 오류 발생", throwable);
                    return Mono.error(AccountException.bankApiError("예금주 조회 중 오류가 발생했습니다."));
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
                .flatMap(response -> {
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
                        
                        return Mono.just(ApiResponse.success(historyResponse));
                    } else {
                        String errorMessage = response.getHeader() != null 
                                ? response.getHeader().getResponseMessage() 
                                : "거래 내역 조회 중 오류가 발생했습니다.";
                        return Mono.error(AccountException.accountNotFound());
                    }
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof AccountException) {
                        return Mono.error(throwable);
                    }
                    log.error("거래 내역 조회 중 오류 발생", throwable);
                    return Mono.error(AccountException.bankApiError("거래 내역 조회 중 오류가 발생했습니다."));
                });
    }
    
    private TransactionDto convertToTransactionDto(com.ssafy.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC rec) {
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
    
    private void getAuthCodeAndSendNotification(String encryptedUserKey, String accountNo, Long userId) {
        CompletableFuture.runAsync(() -> {
            try {
                String authCode = extractAuthCodeFromRecentTransactions(encryptedUserKey, accountNo);
                fcmService.sendOneWonVerificationNotification(userId, accountNo, authCode);
            } catch (Exception e) {
                log.error("인증 코드 추출 중 오류 발생. 기본 알림 전송. userId: {}", userId, e);
                fcmService.sendOneWonVerificationNotification(userId, accountNo, null);
            }
        });
    }
    
    private String extractAuthCodeFromRecentTransactions(String encryptedUserKey, String accountNo) {
        try {
            String userKey = encryptionService.decrypt(encryptedUserKey);

            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            return financialApiService.inquireTransactionHistoryList(
                            userKey, accountNo, todayStr, todayStr, "M", "DESC")
                    .mapNotNull(response -> {
                        if (response.getHeader() != null && "H0000".equals(response.getHeader().getResponseCode())) {
                            return response.getRec().getList().stream()
                                    .filter(transaction -> "1".equals(transaction.getTransactionBalance()))
                                    .filter(transaction -> transaction.getTransactionSummary() != null &&
                                            transaction.getTransactionSummary().contains("티끌"))
                                    .findFirst()
                                    .map(transaction -> transaction.getTransactionSummary())
                                    .orElse(null);
                        }
                        return null;
                    })
                    .block();
        } catch (Exception e) {
            log.error("거래 내역 조회 중 오류 발생", e);
            return null;
        }
    }

    @Override
    public Mono<Void> transferTiggleIfPayMore(String encryptedUserKey, Long userId, Long dutchpayId, long originalAmount, boolean payMoreSelected) {
        if (!payMoreSelected) return Mono.empty();
        long tiggle = calcTiggle(originalAmount);
        if (tiggle <= 0) return Mono.empty();
        return transferTiggleToPiggy(encryptedUserKey, userId, dutchpayId, tiggle);
    }

    @Override
    public Mono<Void> transferTiggleToPiggy(String encryptedUserKey, Long userId, Long dutchpayId, long tiggleAmount) {
        if (tiggleAmount <= 0) return Mono.empty();

        return Mono.fromCallable(() -> {
                    String userKey = encryptionService.decrypt(encryptedUserKey);
                    LinkedAccounts link = getLinkedAccounts(userKey, userId);

                    // ✅ 더치페이 제목 조회 (메모로 사용)
                    String memoTitle = dutchpayRepo.findById(dutchpayId)
                            .map(Dutchpay::getTitle)
                            .filter(t -> t != null && !t.isBlank())
                            .orElse("더치페이");

                    return new Object[]{link, memoTitle};
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(arr -> {
                    LinkedAccounts link = (LinkedAccounts) arr[0];
                    String memoTitle = (String) arr[1];

                    return financialApiService.updateDemandDepositAccountTransfer(
                                    link.userKey(),
                                    link.piggyAccountNo(),
                                    // summary는 기존 태그 유지 (멱등/추적용)
                                    "[DUTCH][PM][UID:" + userId + "] DP" + dutchpayId,
                                    String.valueOf(tiggleAmount),
                                    link.primaryAccountNo(),
                                    // ✅ 메모를 더치페이 title로
                                    memoTitle
                            )
                            .timeout(Duration.ofSeconds(5))
                            .map(resp -> {
                                boolean ok = resp.getHeader() != null && "H0000".equals(resp.getHeader().getResponseCode());
                                log.info("[DUTCH][PM] transfer result ok={}, userId={}, dpId={}, amount={}",
                                        ok, userId, dutchpayId, tiggleAmount);
                                if (!ok) {
                                    String msg = resp.getHeader() == null ? "응답 헤더 없음" : resp.getHeader().getResponseMessage();
                                    throw new IllegalStateException("이체 실패: " + msg);
                                }
                                return resp;
                            });
                })
                .flatMap(resp ->
                        Mono.fromCallable(() -> {
                            piggyBankWriterService.applyTiggle(userId, BigDecimal.valueOf(tiggleAmount));
                            return true;
                        }).subscribeOn(Schedulers.boundedElastic())
                )
                .then()
                .onErrorResume(org.springframework.web.reactive.function.client.WebClientResponseException.class, e -> {
                    log.warn("더치페이 자투리 저축 실패 userId={}, dutchpayId={}, status={}, headers={}, body={}",
                            userId, dutchpayId, e.getStatusCode(), e.getHeaders(), e.getResponseBodyAsString(), e);
                    return Mono.error(e);
                })
                .onErrorResume(e -> {
                    log.warn("더치페이 자투리 저축 실패(일반) userId={}, dutchpayId={}, msg={}", userId, dutchpayId, e.getMessage(), e);
                    return Mono.error(e);
                });
    }



    private LinkedAccounts getLinkedAccounts(String userKey, Long userId) {
        var piggy = piggyBankRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new IllegalStateException("Piggy bank not found for user: " + userId));

        var user = studentRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        String primary = user.getPrimaryAccountNo();
        String piggyAcc = piggy.getAccountNo();

        if (userKey == null || userKey.isBlank())
            throw new IllegalStateException("userKey not linked for user: " + userId);
        if (primary == null || primary.isBlank())
            throw new IllegalStateException("primaryAccountNo not linked for user: " + userId);
        if (piggyAcc == null || piggyAcc.isBlank())
            throw new IllegalStateException("piggy account_no not linked for user: " + userId);

        return new LinkedAccounts(userKey, primary, piggyAcc);
    }

    private long calcTiggle(long original) {
        if (original <= 0) return 0L;
        long rounded = ((original + 99L) / 100L) * 100L;
        return Math.max(0L, rounded - original);
    }

    @Override
    public Mono<Void> payDutchShare(String encryptedUserKey, Long dutchpayId, Long userId, boolean payMore) {
        return Mono.fromCallable(() -> {
                    Dutchpay dp = dutchpayRepo.findById(dutchpayId)
                            .orElseThrow(() -> new IllegalStateException("더치페이 없음"));
                    DutchpayShare share = shareRepo.findByDutchpayIdAndUserId(dutchpayId, userId)
                            .orElseThrow(() -> new IllegalStateException("참여 정보 없음"));

                    // ✅ 멱등: 이미 PAID면 즉시 종료
                    if (share.getStatus() == DutchpayShareStatus.PAID) {
                        log.info("[Dutchpay][pay] already PAID — skip. dpId={}, userId={}", dutchpayId, userId);
                        return new Object[]{dp, share, null}; // signal for early exit
                    }

                    long base = share.getAmount();
                    long topUp = 0L;
                    if (payMore) {
                        Long rpp = dp.getRoundedPerPerson(); // null이면 100원 올림
                        if (rpp == null || rpp < base) topUp = calcTiggle(base);
                        else topUp = Math.max(0L, rpp - base);
                    }

                    log.info("[Dutchpay][pay] start. dpId={}, userId={}, base={}, topUp={}, payMore={}",
                            dutchpayId, userId, base, topUp, payMore);

                    return new Object[]{dp, share, topUp};
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(arr -> {
                    Dutchpay dp = (Dutchpay) arr[0];
                    DutchpayShare share = (DutchpayShare) arr[1];
                    Long topUp = (Long) arr[2];

                    // ✅ early exit: 이미 PAID로 들어온 케이스
                    if (topUp == null) return Mono.empty();

                    boolean isCreator = dp.getCreator() != null && dp.getCreator().getId().equals(userId);

                    if (isCreator) {
                        // 생성자: 지분 이체 없음, payMore면 저축만
                        Mono<Void> topup = (topUp > 0)
                                ? transferTiggleToPiggy(encryptedUserKey, userId, dutchpayId, topUp)
                                .onErrorResume(e -> {
                                    log.warn("[PayMore][creator] 실패 userId={}, dpId={}, {}", userId, dutchpayId, e.getMessage());
                                    return Mono.empty(); // 저축 실패는 결제 자체 실패로 보지 않음
                                })
                                : Mono.empty();

                        return topup.then(
                                Mono.fromCallable(() -> {
                                    // ✅ 성공 후 확정 저장
                                    share.settle(payMore, topUp);
                                    shareRepo.saveAndFlush(share);

                                    long unpaid = shareRepo.countByDutchpayIdAndStatusNot(dutchpayId, DutchpayShareStatus.PAID);
                                    if (unpaid == 0 && !"COMPLETED".equalsIgnoreCase(dp.getStatus())) {
                                        dp.setStatus("COMPLETED");
                                        dutchpayRepo.saveAndFlush(dp);
                                        log.info("[Dutchpay] 모든 참가자 납부 완료 → COMPLETED (dpId={})", dutchpayId);
                                    }
                                    return true;
                                }).subscribeOn(Schedulers.boundedElastic())
                        ).then();
                    } else {
                        // 참여자 → 생성자에게 지분 이체
                        Long creatorId = dp.getCreator().getId();
                        Users creator = studentRepository.findById(creatorId)
                                .orElseThrow(() -> new IllegalStateException("생성자 사용자 없음"));
                        String creatorPrimary = creator.getPrimaryAccountNo();
                        if (creatorPrimary == null || creatorPrimary.isBlank()) {
                            return Mono.error(new IllegalStateException("생성자 주계좌 미등록"));
                        }

                        String userKey = encryptionService.decrypt(encryptedUserKey);
                        LinkedAccounts payerLink = getLinkedAccounts(userKey, userId);

                        // 1) 지분(원금) 이체
                        return financialApiService.updateDemandDepositAccountTransfer(
                                        payerLink.userKey(),
                                        creatorPrimary, // 입금: 생성자 주계좌
                                        "[DUTCH][PAY] DP" + dutchpayId + " U" + userId,
                                        String.valueOf(share.getAmount()),
                                        payerLink.primaryAccountNo(), // 출금: 참여자 주계좌
                                        "더치페이 납부 DP" + dutchpayId + " U" + userId
                                )
                                .timeout(Duration.ofSeconds(5))
                                .flatMap(resp -> {
                                    boolean ok = resp.getHeader() != null && "H0000".equals(resp.getHeader().getResponseCode());
                                    if (!ok) {
                                        String msg = resp.getHeader() == null ? "응답 헤더 없음" : resp.getHeader().getResponseMessage();
                                        return Mono.error(new IllegalStateException("지분 이체 실패: " + msg));
                                    }

                                    // 2) payMore면 저축
                                    Mono<Void> topup = (topUp > 0)
                                            ? transferTiggleToPiggy(encryptedUserKey, userId, dutchpayId, topUp)
                                            .onErrorResume(e -> {
                                                log.warn("[PayMore] 실패 userId={}, dpId={}, {}", userId, dutchpayId, e.getMessage());
                                                return Mono.empty();
                                            })
                                            : Mono.empty();

                                    // 3) ✅ 성공 후 확정 저장
                                    return topup.then(
                                            Mono.fromCallable(() -> {
                                                share.settle(payMore, topUp);
                                                shareRepo.saveAndFlush(share);

                                                long unpaid = shareRepo.countByDutchpayIdAndStatusNot(dutchpayId, DutchpayShareStatus.PAID);
                                                if (unpaid == 0 && !"COMPLETED".equalsIgnoreCase(dp.getStatus())) {
                                                    dp.setStatus("COMPLETED");
                                                    dutchpayRepo.saveAndFlush(dp);
                                                    log.info("[Dutchpay] 모든 참가자 납부 완료 → COMPLETED (dpId={})", dutchpayId);
                                                }
                                                return true;
                                            }).subscribeOn(Schedulers.boundedElastic())
                                    );
                                });
                    }
                })
                .then();
    }


}