package com.ssafy.tiggle.service.donation;

import com.ssafy.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.donation.request.DonationRequest;
import com.ssafy.tiggle.dto.donation.response.*;
import com.ssafy.tiggle.entity.*;
import com.ssafy.tiggle.exception.GlobalExceptionHandler;
import com.ssafy.tiggle.exception.donation.DonationException;
import com.ssafy.tiggle.repository.donation.*;
import com.ssafy.tiggle.repository.esg.EsgCategoryRepository;
import com.ssafy.tiggle.repository.university.UniversityRepository;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.ssafy.tiggle.service.finopenapi.FinancialApiService;
import com.ssafy.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
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
    private final DonationOrganizationRepository donationOrganizationRepository;
    private final EsgCategoryRepository esgCategoryRepository;
    private final UniversityRepository universityRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final DonationRankingStore rankingStore;

    private final long LEVEL_AMOUNT = 100;

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
                                return financialApiService.updateDemandDepositAccountTransfer(userKey, depositAccountNo, user.getName(), request.getAmount().toString(), userAccountNo, "[DONATION] " + request.getCategory().toString() + " 기부")
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

                                        // 7. 하트 지급
                                        UserCharacter character = userCharacterRepository.findByUserId(userId)
                                                .orElseThrow(() -> new IllegalArgumentException("캐릭터가 존재하지 않습니다."));
                                        character.addHeart(request.getAmount());
                                        userCharacterRepository.save(character);

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

        UserCharacter userCharacter = userCharacterRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터가 존재하지 않습니다."));

        // 총 기부금액 조회
        BigDecimal totalAmountBD = donationHistoryRepository.findTotalAmountByUserId(userId);
        long totalAmount = totalAmountBD != null ? totalAmountBD.longValue() : 0L;

        // 경험치
        long experiencePoints = userCharacter.getExperiencePoints();

        // 하트
        Integer heart = userCharacter.getHeart();

        // 레벨
        int level = userCharacter.getLevel();

        return new DonationGrowthLevel(totalAmount, experiencePoints, LEVEL_AMOUNT, level, heart);
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

    @Override
    public List<DonationRanking> getUniversityRanking() {

        List<RankingProjection> list = donationHistoryRepository.getUniversityRanking();

        return list.stream()
                .map(dto -> new DonationRanking(
                        dto.getRank(),
                        dto.getName(),
                        dto.getAmount()
                ))
                .toList();
    }

    @Override
    public List<DonationRanking> getDepartmentRanking(Long userId) {

        Users user = studentRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Long universityId = Optional.ofNullable(user.getUniversity())
                .map(University::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 소속 학교 정보가 없습니다."));

        List<RankingProjection> list = donationHistoryRepository.getDepartmentRanking(universityId);

        return list.stream()
                .map(dto -> new DonationRanking(
                        dto.getRank(),
                        dto.getName(),
                        dto.getAmount()
                ))
                .toList();
    }

    // 학교단위 기부
    @Override
    @Transactional
    public void transferDonations() {

        List<University> universities = universityRepository.findAll();

        for (University uni : universities) {
            transferThemeDonation(uni, Category.PLANET.getId(), uni.getPlanetAccountNo());
            transferThemeDonation(uni, Category.PEOPLE.getId(), uni.getPeopleAccountNo());
            transferThemeDonation(uni, Category.PROSPERITY.getId(), uni.getProsperityAccountNo());
        }
    }

    // 학교 테마별 계좌 -> 기부단체
    private void transferThemeDonation(University university, Long categoryId, String uniAccountNo) {

        Mono.fromCallable(() -> {

                    // 1. 단체 목록 조회
                    List<DonationOrganization> organizations = donationOrganizationRepository.findByEsgCategory_id(categoryId);

                    if (organizations == null || organizations.isEmpty()) {
                        throw DonationException.organizationAccountNotFound();
                    }

                    // 2. 학교 계좌 확인
                    if (uniAccountNo == null || uniAccountNo.isBlank()) {
                        logger.error("학교의 계좌 정보가 없습니다");
                        throw DonationException.universityAccountNotFound();
                    } else {
                        logger.info("학교 계좌: {}", uniAccountNo);
                    }

                    return organizations;
                })
                .flatMapMany(organizations -> {

                    String userKey = encryptionService.decrypt(university.getUserKey());
                    if (userKey == null || userKey.isBlank()) {
                        logger.error("학교의 계정 정보가 없습니다");
                        throw DonationException.universityAccountNotFound();
                    }

                    // 3. 계좌 잔고 확인
                    return financialApiService.inquireDemandDepositAccountBalance(userKey, uniAccountNo)
                            .flatMapMany(balanceResponse -> {
                                long totalBalance = Long.parseLong(balanceResponse.getRec().getAccountBalance());
                                if (totalBalance < 1000) {
                                    logger.warn("계좌 잔고 부족: {}", totalBalance);
                                    return Mono.empty();
                                } else {
                                    logger.info("{} {} 계좌 잔고: {}", university.getName(), categoryId, totalBalance);
                                }

                                // 4. 단체 수로 나눠서 1/N씩 기부
                                int orgCount = organizations.size();
                                Long amountPerOrg = totalBalance / orgCount;

                                // 5. 싸피 금융 API - 계좌이체 실행
                                return Flux.fromIterable(organizations)
                                        .flatMap(org -> {

                                            String orgAccountNo = org.getAccountNo();
                                            if (orgAccountNo == null || orgAccountNo.isBlank()) {
                                                logger.error("기부단체의 계좌 정보가 없습니다");
                                                return Mono.error(DonationException.organizationAccountNotFound());
                                            } else {
                                                logger.info("기부단체 계좌: {}", orgAccountNo);
                                            }

                                            logger.info("이체: {} -> {} amount={}", uniAccountNo, org.getAccountNo(), amountPerOrg);
                                            return financialApiService.updateDemandDepositAccountTransfer(
                                                    userKey,
                                                    orgAccountNo,
                                                    university.getName(),
                                                    amountPerOrg.toString(),
                                                    uniAccountNo,
                                                    org.getName()
                                            ).onErrorMap(e -> DonationException.externalApiFailure());
                                        });
                            });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> logger.info("계좌이체 성공: {}", result),
                        error -> logger.error("계좌이체 실패: ", error)
                );
    }

    // 기부 랭킹 캐싱
    @Override
    @Transactional(readOnly = true)
    public void updateRankingCache() {

        List<Object[]> results = donationHistoryRepository.sumByUniversity();
        rankingStore.saveUniversityRanking(results);

        List<University> universities = universityRepository.findAll();

        for (University uni : universities) {
            rankingStore.saveDepartmentRanking(uni.getId(), donationHistoryRepository.sumByDepartment(uni.getId()));
        }
    }


    @Transactional
    @Override
    public CharacterLevel useHeart(Long userId) {

        UserCharacter character = userCharacterRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("캐릭터가 존재하지 않습니다."));

        Long newExperiencePoints = character.getExperiencePoints() + 100;
        Integer level = character.getLevel();
        Integer heart = character.getHeart();

        if (character.getHeart() < 1) {
            throw new IllegalArgumentException("하트 부족!");
        }

        // 하트 감소 & 경험치 증가
        character.setHeart(--heart);

        // 레벨 재조정
        if (newExperiencePoints >= LEVEL_AMOUNT) {
            character.setLevel(++level);
            newExperiencePoints = LEVEL_AMOUNT;
        }
        character.setExperiencePoints(newExperiencePoints);

        return new CharacterLevel(newExperiencePoints, LEVEL_AMOUNT, level, heart);
    }
}