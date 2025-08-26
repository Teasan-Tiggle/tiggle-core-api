package com.example.tiggle.service.donation;

import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.dto.donation.response.DonationGrowthLevel;
import com.example.tiggle.dto.donation.response.DonationHistoryResponse;
import com.example.tiggle.dto.donation.response.DonationStatus;
import com.example.tiggle.dto.donation.response.DonationSummary;
import com.example.tiggle.entity.DonationHistory;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.University;
import com.example.tiggle.entity.Users;
import com.example.tiggle.exception.DonationException;
import com.example.tiggle.exception.GlobalExceptionHandler;
import com.example.tiggle.repository.donation.DonationHistoryRepository;
import com.example.tiggle.repository.donation.SummaryProjection;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public Mono<ApiResponse<PrimaryAccountInfoDto>> getDonation(Long userId, String encryptedUserKey) {
        return Mono.fromCallable(() -> {

                    Users user = studentRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

                    String userAccountNo = user.getPrimaryAccountNo();
                    if (userAccountNo == null || userAccountNo.isBlank()) {
                        throw DonationException.primaryAccountNotFound();
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
                                    logger.info("기부하기 계좌 조회 성공");
                                    return ApiResponse.success(accountInfo);
                                } else {
                                    throw DonationException.externalApiFailure();
                                }
                            })
                            .onErrorResume(throwable -> {
                                throw DonationException.externalApiFailure();
                            });
                });
    }

    @Override
    @Transactional
    public Mono<ApiResponse<Object>> createDonation(Long userId, String encryptedUserKey, DonationRequest request) {

        return Mono.fromCallable(() -> {

                    String userKey = encryptionService.decrypt(encryptedUserKey);

                    // 1. 내 계좌 정보 (기부자)
                    Users user = studentRepository.findByIdWithUniversity(userId)
                            .orElseThrow(() -> new IllegalArgumentException("기부자 정보를 찾을 수 없습니다."));

                    String userAccountNo = user.getPrimaryAccountNo();
                    if (userAccountNo == null || userAccountNo.isBlank()) {
                        logger.error("사용자의 주계좌 정보가 없습니다");
                        throw DonationException.userAccountNotFound();
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

    @Override
    public DonationStatus getUserDonationStatus(Long userId) {
        // 1. Map 초기화: 0값 보장
        Map<String, Long> map = new HashMap<>(Map.of(
                "Planet", 0L,
                "People", 0L,
                "Prosperity", 0L
        ));

        // 2. DB에서 Projection 결과를 가져와 덮어쓰기
        donationHistoryRepository.findTotalAmountByCategoryAndUser(userId)
                .forEach(d -> map.put(d.getCategory(), d.getTotal().longValue()));

        // 3. DTO(record) 반환
        return new DonationStatus(
                map.get("Planet"),
                map.get("People"),
                map.get("Prosperity")
        );
    }

    @Override
    public DonationStatus getUniversityDonationStatus(Long userId) {

        Users user = studentRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Long universityId = Optional.ofNullable(user.getUniversity())
                .map(University::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 소속 학교 정보가 없습니다."));

        // 1. Map 초기화: 0값 보장
        Map<String, Long> map = new HashMap<>(Map.of(
                "Planet", 0L,
                "People", 0L,
                "Prosperity", 0L
        ));

        // 2. DB에서 Projection 결과를 가져와 덮어쓰기
        donationHistoryRepository.findTotalAmountByCategoryAndUniversity(universityId)
                .forEach(d -> map.put(d.getCategory(), d.getTotal().longValue()));

        // 3. DTO(record) 반환
        return new DonationStatus(
                map.get("Planet"),
                map.get("People"),
                map.get("Prosperity")
        );
    }

    @Override
    public DonationStatus getTotalDonationStatus() {
        // 1. Map 초기화: 0값 보장
        Map<String, Long> map = new HashMap<>(Map.of(
                "Planet", 0L,
                "People", 0L,
                "Prosperity", 0L
        ));

        // 2. DB에서 Projection 결과를 가져와 덮어쓰기
        donationHistoryRepository.findTotalAmountByCategory()
                .forEach(d -> map.put(d.getCategory(), d.getTotal().longValue()));

        // 3. DTO(record) 반환
        return new DonationStatus(
                map.get("Planet"),
                map.get("People"),
                map.get("Prosperity")
        );
    }

    @Override
    public DonationGrowthLevel getDonationGrowthLevel(Long userId) {

        final int LEVEL_AMOUNT = 5_000;

        // 1. 총 기부금액 조회
        BigDecimal totalAmountBD = donationHistoryRepository.findTotalAmountByUserId(userId);
        long totalAmount = totalAmountBD != null ? totalAmountBD.longValue() : 0L;

        // 2. 레벨 계산
        int level = (int) (totalAmount / LEVEL_AMOUNT);

        // 3. 다음 레벨까지 남은 금액
        long toNextLevel = LEVEL_AMOUNT - (totalAmount % LEVEL_AMOUNT);

        return new DonationGrowthLevel(totalAmount, toNextLevel, level);
    }

    @Override
    public DonationSummary getUserDonationSummary(Long userId) {

        Users user = studentRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        SummaryProjection summary = donationHistoryRepository.findDonationSummaryByUserId(userId);
        Integer universityRank = donationHistoryRepository.findUniversityRank(user.getUniversity().getId());

        return new DonationSummary(
                summary.getTotalAmount() != null ? summary.getTotalAmount().longValue() : 0L,
                summary.getMonthlyAmount() != null ? summary.getMonthlyAmount().longValue() : 0L,
                summary.getCategoryCnt() != null ? summary.getCategoryCnt() : 0,
                universityRank
        );
    }
}