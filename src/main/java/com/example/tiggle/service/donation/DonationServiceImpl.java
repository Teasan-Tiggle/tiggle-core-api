package com.example.tiggle.service.donation;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.dto.donation.response.DonationHistoryResponse;
import com.example.tiggle.entity.DonationHistory;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.Users;
import com.example.tiggle.exception.DonationException;
import com.example.tiggle.exception.GlobalExceptionHandler;
import com.example.tiggle.repository.donation.DonationHistoryRepository;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;
    private final StudentRepository studentRepository;
    private final DonationHistoryRepository donationHistoryRepository;
    private final EsgCategoryRepository esgCategoryRepository;

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    @Transactional
    public Mono<ApiResponse<Object>> createDonation(Long userId, String encryptedUserKey, DonationRequest request) {

        return Mono.fromCallable(() -> {

                    String userKey = encryptionService.decrypt(encryptedUserKey);

                    // 1. 내 계좌 정보 (기부자)
                    Users user = studentRepository.findByIdWithUniversity(userId)
                            .orElseThrow(() -> new IllegalArgumentException("기부자 정보를 찾을 수 없습니다."));

                    String userAccountNo = user.getPrimaryAccountNo();
                    if (userAccountNo == null) {
                        logger.error("사용자의 주계좌 정보가 없습니다");
                        throw DonationException.universityAccountNotFound();
                    } else {
                        logger.info("사용자의 주계좌: {}", userAccountNo);
                    }

                    // 2. 학교 테마 계좌 정보 (기부처)
                    String depositAccountNo = switch (request.getCategory()) {
                        case PLANET -> user.getUniversity().getPlanetAccountNo();
                        case PEOPLE -> user.getUniversity().getPeopleAccountNo();
                        case PROSPERITY -> user.getUniversity().getProsperityAccountNo();
                    };

                    if (depositAccountNo == null || depositAccountNo.isEmpty() || depositAccountNo.isBlank()) {
                        logger.error("학교의 기부 계좌 정보가 없습니다");
                        throw DonationException.universityAccountNotFound();
                    } else {
                        logger.info("학교의 기부 계좌: {}", depositAccountNo);
                    }

                    return new Object[]{userKey, user, userAccountNo, depositAccountNo};
                })
                .flatMap(obj -> {

                    String userKey = (String) ((Object[]) obj)[0];
                    Users user = (Users) ((Object[]) obj)[1];
                    String userAccountNo = (String) ((Object[]) obj)[2];
                    String depositAccountNo = (String) ((Object[]) obj)[3];

                    // 3. 계좌 잔고 확인
                    return financialApiService.inquireDemandDepositAccountBalance(userKey, user.getPrimaryAccountNo())
                            .flatMap(balanceResponse -> {
                                Long balance = Long.parseLong(balanceResponse.getRec().getAccountBalance());
                                if (balance < request.getAmount()) {
                                    logger.error("계좌 잔고 부족: {}", balanceResponse);
                                    throw DonationException.accountBalance(balance);
                                }

                                // 4. 싸피 금융 API - 계좌이체 실행
                                return financialApiService.updateDemandDepositAccountTransfer(userKey, depositAccountNo, user.getName(), request.getAmount().toString(), userAccountNo, request.getCategory().toString())
                                        .thenReturn(new Object[]{userKey, user, userAccountNo});
                            });
                })
                .flatMap(obj -> {

                    String userKey = (String) ((Object[]) obj)[0];
                    Users user = (Users) ((Object[]) obj)[1];
                    String userAccountNo = (String) ((Object[]) obj)[2];

                    // 5. 거래내역 조회
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

                    String start = LocalDate.now().minusDays(1).format(formatter);
                    String end = LocalDate.now().plusDays(1).format(formatter);

                    return financialApiService.inquireTransactionHistoryList(userKey, userAccountNo, start, end, "A", "DESC")
                            .map(response -> response.getRec().getList().get(0)) // 최신 거래 1건
                            .flatMap(latest ->

                                    Mono.fromCallable(() -> {

                                        // 6. 기부내역 DB 저장
                                        EsgCategory esgCategory = esgCategoryRepository.findById(request.getCategory().getId())
                                                .orElseThrow(() -> new IllegalArgumentException("카테고리 값을 찾을 수 없습니다."));

                                        LocalDateTime donatedAt = LocalDateTime.parse(
                                                latest.getTransactionDate() + "T" + latest.getTransactionTime(),
                                                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                                        );

                                        DonationHistory donationHistory = DonationHistory.builder()
                                                .user(user)
                                                .accountType("주계좌")
                                                .accountNo(userAccountNo)
                                                .esgCategory(esgCategory)
                                                .amount(BigDecimal.valueOf(request.getAmount()))
                                                .donatedAt(donatedAt)
                                                .title(request.getCategory().toString())
                                                .build();

                                        donationHistoryRepository.save(donationHistory);

                                        logger.info("기부하기 성공");

                                        return ApiResponse.success();
                                    }).subscribeOn(Schedulers.boundedElastic())
                            );
                }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public ApiResponse<List<DonationHistoryResponse>> getDonationHistory(Long userId) {
        List<DonationHistoryResponse> histories = donationHistoryRepository.findByUser_IdOrderByDonatedAtDesc(userId)
                .stream()
                .map(donation -> DonationHistoryResponse.builder()
                        .category(donation.getEsgCategory().getName())
                        .amount(donation.getAmount().longValue())
                        .donatedAt(donation.getDonatedAt())
                        .title(donation.getTitle())
                        .build())
                .toList();

        return ApiResponse.success(histories);
    }
}