package com.ssafy.tiggle.service.piggybank;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC;
import com.ssafy.tiggle.dto.finopenapi.response.InquireTransactionHistoryListResponse;
import com.ssafy.tiggle.dto.piggybank.request.CreatePiggyBankRequest;
import com.ssafy.tiggle.dto.piggybank.request.PiggyBankEntriesPageRequest;
import com.ssafy.tiggle.dto.piggybank.request.UpdatePiggyBankSettingsRequest;
import com.ssafy.tiggle.dto.piggybank.response.*;
import com.ssafy.tiggle.entity.EsgCategory;
import com.ssafy.tiggle.entity.PiggyBank;
import com.ssafy.tiggle.entity.Users;
import com.ssafy.tiggle.repository.esg.EsgCategoryRepository;
import com.ssafy.tiggle.repository.piggybank.PiggyBankRepository;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.ssafy.tiggle.service.finopenapi.FinancialApiService;
import com.ssafy.tiggle.service.security.EncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggyBankServiceImpl implements PiggyBankService {

    private final PiggyBankRepository piggyBankRepository;
    private final EsgCategoryRepository esgCategoryRepository;
    private final StudentRepository studentRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YMD_NUM = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HMS_NUM = DateTimeFormatter.ofPattern("HHmmss");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter KOR_MONTH_DAY = DateTimeFormatter.ofPattern("M월 d일").withLocale(Locale.KOREAN);

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(Long userId) {
        return Mono.fromCallable(() ->
                        piggyBankRepository.findByOwner_Id(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."))
                ).map(this::toResponse)
                .map(ApiResponse::success)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> updateSettings(Long userId, UpdatePiggyBankSettingsRequest req) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            if (req.getName() != null)          piggy.setName(req.getName());
            if (req.getTargetAmount() != null)  piggy.setTargetAmount(req.getTargetAmount());
            if (req.getAutoDonation() != null)  piggy.setAutoDonation(req.getAutoDonation());
            if (req.getAutoSaving() != null)    piggy.setAutoSaving(req.getAutoSaving());

            if (req.getEsgCategoryId() != null) {
                if (req.getEsgCategoryId() <= 0) {
                    piggy.setEsgCategory(null);
                } else {
                    EsgCategory cat = esgCategoryRepository.findById(req.getEsgCategoryId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 ESG 카테고리입니다."));
                    piggy.setEsgCategory(cat);
                }
            }

            piggyBankRepository.save(piggy);
            return ApiResponse.success(toResponse(piggy));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> setCategory(Long userId, Long categoryId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            EsgCategory cat = esgCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 ESG 카테고리입니다."));
            piggy.setEsgCategory(cat);
            piggyBankRepository.save(piggy);

            return ApiResponse.success(toResponse(piggy));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Long userId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            piggy.setEsgCategory(null);
            piggyBankRepository.save(piggy);

            return ApiResponse.success(toResponse(piggy));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PiggyBankResponse toResponse(PiggyBank p) {
        return new PiggyBankResponse(
                p.getId(),
                p.getName(),
                p.getCurrentAmount(),
                p.getTargetAmount(),
                p.getSavingCount(),
                p.getDonationCount(),
                p.getDonationTotalAmount(),
                p.getAutoDonation(),
                p.getAutoSaving(),
                p.getEsgCategory() == null ? null :
                        new EsgCategoryDto(
                                p.getEsgCategory().getId(),
                                p.getEsgCategory().getName(),
                                p.getEsgCategory().getDescription(),
                                p.getEsgCategory().getCharacterName()
                        )
        );
    }

    @Override
    @Transactional
    public Mono<ApiResponse<PiggyBankSummaryResponse>> create(String encryptedUserKey, Long userId, CreatePiggyBankRequest req) {
        String userKey = encryptionService.decrypt(encryptedUserKey);

        // 기존 스타일 유지: DB는 트랜잭션 범위 내, 외부 API는 boundedElastic에서 block()
        return Mono.fromCallable(() -> {
            piggyBankRepository.findByOwner_Id(userId).ifPresent(pb -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 개설된 저금통이 있습니다.");
            });

            Users owner = studentRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."));

            var finRes = financialApiService.createDemandDepositAccount(userKey)
                    .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융API 호출 실패", e))
                    .block();

            if (finRes == null || finRes.getHeader() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융API 응답이 올바르지 않습니다.");
            }
            if (!"H0000".equals(finRes.getHeader().getResponseCode())) {
                String msg = finRes.getHeader().getResponseMessage();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융 계좌 개설 실패: " + (msg != null ? msg : "알 수 없는 오류"));
            }

            String createdAccountNo = finRes.getRec() != null ? finRes.getRec().getAccountNo() : null;
            if (createdAccountNo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "생성된 계좌번호가 누락되었습니다.");
            }
            log.info("[Piggy][Create] userId={}, createdAccountNo(masked)={}", userId, maskAccount(createdAccountNo));

            PiggyBank pb = PiggyBank.builder()
                    .owner(owner)
                    .name(req.getName())
                    .targetAmount(req.getTargetAmount())
                    .accountNo(createdAccountNo)
                    .build();

            if (req.getEsgCategoryId() != null) {
                EsgCategory cat = esgCategoryRepository.findById(req.getEsgCategoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "ESG 카테고리를 찾을 수 없습니다."));
                pb.setEsgCategory(cat);
            }

            PiggyBank saved = piggyBankRepository.save(pb);

            PiggyBankSummaryResponse body =
                    new PiggyBankSummaryResponse(saved.getName(), saved.getCurrentAmount(), BigDecimal.ZERO);

            return ApiResponse.success(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankSummaryResponse>> getSummary(String encryptedUserKey, Long userId) {
        return Mono.fromCallable(() ->
                piggyBankRepository.findByOwner_Id(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."))
        ).flatMap(piggy -> {
            if (piggy.getAccountNo() == null || piggy.getAccountNo().isBlank()) {
                return Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "저금통 계좌가 없습니다. 먼저 개설해주세요."
                ));
            }
            final String userKey = encryptionService.decrypt(encryptedUserKey);
            return lastWeekSavedAmount(userKey, piggy.getAccountNo(), userId)
                    .map(lastWeek -> ApiResponse.success(
                            new PiggyBankSummaryResponse(piggy.getName(), piggy.getCurrentAmount(), lastWeek)
                    ));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 지난주(월~일) 적립 합계 (입금만 + 사용자/자투리 태그 매칭) — userKey는 평문 */
    private Mono<BigDecimal> lastWeekSavedAmount(String userKeyPlain, String accountNo, Long userId) {
        LocalDate today = LocalDate.now(KST);
        LocalDate lastWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekEnd   = lastWeekStart.with(DayOfWeek.SUNDAY);

        String start = lastWeekStart.format(YMD);
        String end   = lastWeekEnd.format(YMD);

        return financialApiService
                .inquireTransactionHistoryList(userKeyPlain, accountNo, start, end, "A", "ASC")
                .map(res -> {
                    if (res == null || res.getRec() == null || res.getRec().getList() == null) {
                        return BigDecimal.ZERO;
                    }
                    return res.getRec().getList().stream()
                            .filter(this::isDeposit)
                            .filter(r -> isUserSavingRecord(r, userId)) // 사용자 태그 + 자투리/더치페이 태그
                            .map(this::txAmount)                         // 거래금액 기준
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .onErrorResume(e -> {
                    log.error("[Piggy][Summary] 지난주 적립액 조회 실패 → 0으로 대체 (accountNo={})", maskAccount(accountNo), e);
                    return Mono.just(BigDecimal.ZERO);
                });
    }

    @Override
    public Mono<ApiResponse<PiggyBankEntriesPageResponse>> getEntriesPage(
            String encryptedUserKey, Long userId, PiggyBankEntriesPageRequest req) {

        return Mono.fromCallable(() ->
                piggyBankRepository.findByOwner_Id(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."))
        ).flatMap(piggy -> {
            if (piggy.getAccountNo() == null || piggy.getAccountNo().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "저금통 계좌가 없습니다. 먼저 개설해주세요.");
            }

            LocalDate from = req.getFrom() != null ? LocalDate.parse(req.getFrom()) : LocalDate.now(KST).minusDays(30);
            LocalDate to   = req.getTo()   != null ? LocalDate.parse(req.getTo())   : LocalDate.now(KST);
            int size = Math.min(Math.max(req.getSize() == null ? 20 : req.getSize(), 1), 100);

            final String userKey = encryptionService.decrypt(encryptedUserKey);

            return financialApiService.inquireTransactionHistoryList(
                            userKey, piggy.getAccountNo(),
                            from.format(YMD), to.format(YMD),
                            "A", "ASC"
                    )
                    .map(res -> {
                        List<SimpleTx> all = mapToSimpleTxList(res);

                        String standard = toStandardType(req.getType()); // "TIGGLE" | "DUTCHPAY" | "ALL"
                        List<SimpleTx> filteredSorted = all.stream()
                                .filter(tx -> "IN".equals(tx.typeCode))
                                .filter(tx -> matchTypeBySummary(tx, standard, userId)) // summary+memo 동시 검사
                                .sorted(Comparator
                                        .comparing((SimpleTx t) -> t.occurredAt).reversed()
                                        .thenComparing(t -> t.id, Comparator.reverseOrder()))
                                .toList();

                        // TIGGLE만 월별 순번을 붙이기 위해 먼저 전체 리스트 기준으로 월별 순번산정
                        Map<String, Integer> monthlyOrdinals = computeMonthlyOrdinals(filteredSorted);

                        Cursor cur = decodeCursor(req.getCursor());
                        if (cur != null) filteredSorted = filteredSorted.stream().filter(tx -> isBeforeCursor(tx, cur)).toList();

                        // size+1만큼만 잘라 hasNext 계산
                        List<SimpleTx> sliced = filteredSorted.stream().limit(size + 1).toList();
                        boolean hasNext = sliced.size() > size;
                        List<SimpleTx> page = hasNext ? sliced.subList(0, size) : sliced;

                        String nextCursor = hasNext ? encodeCursor(page.get(page.size() - 1)) : null;

                        ZoneOffset offset = KST.getRules().getOffset(Instant.now());

                        List<PiggyBankEntryItemDto> items = page.stream().map(tx -> {
                                    // ALL이면 각 트랜잭션별 실제 타입 라벨
                                    String typeForTx = "ALL".equals(standard) ? classifyType(tx) : standard;
                                    Integer nth = ("TIGGLE".equals(typeForTx) ? monthlyOrdinals.get(tx.id) : null);
                                    String title = buildMonthlyTitle(tx.occurredAt.toLocalDate(), typeForTx, nth);
                                    String occurredDateLabel = tx.occurredAt.toLocalDate().format(KOR_MONTH_DAY);
                                    return new PiggyBankEntryItemDto(
                                            tx.id,
                                            typeForTx,
                                            tx.amount,
                                            occurredDateLabel,
                                            title
                                    );
                                })
                                .toList();

                        return ApiResponse.success(new PiggyBankEntriesPageResponse(items, nextCursor, items.size(), hasNext));
                    })
                    .onErrorResume(e -> {
                        log.error("[Piggy][EntriesPage] 조회 실패 (accountNo={})", maskAccount(piggy.getAccountNo()), e);
                        return Mono.just(ApiResponse.success(new PiggyBankEntriesPageResponse(List.of(), null, 0, false)));
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ===== 내부 유틸 =====

    private List<SimpleTx> mapToSimpleTxList(InquireTransactionHistoryListResponse res) {
        if (res == null || res.getRec() == null || res.getRec().getList() == null) return List.of();

        List<InquireTransactionHistoryListREC> rows = res.getRec().getList();
        List<SimpleTx> out = new ArrayList<>(rows.size());

        for (InquireTransactionHistoryListREC r : rows) {
            String d = (r.getTransactionDate() == null || r.getTransactionDate().isBlank()) ? "19700101" : r.getTransactionDate();
            String t = (r.getTransactionTime() == null || r.getTransactionTime().isBlank()) ? "000000" : r.getTransactionTime();

            LocalDate date = LocalDate.parse(d, YMD_NUM);
            LocalTime time = LocalTime.parse(t, HMS_NUM);
            LocalDateTime occurredAt = LocalDateTime.of(date, time);

            BigDecimal amount = txAmount(r);
            if (isWithdrawal(r) && amount.signum() > 0) amount = amount.negate();

            SimpleTx tx = new SimpleTx();
            tx.id         = r.getTransactionUniqueNo();
            tx.occurredAt = occurredAt;
            tx.amount     = amount;
            tx.summary    = Optional.ofNullable(r.getTransactionSummary()).orElse("");
            tx.memo       = Optional.ofNullable(r.getTransactionMemo()).orElse("");
            tx.typeCode   = isDeposit(r) ? "IN" : (isWithdrawal(r) ? "OUT" : "ETC");
            out.add(tx);
        }
        return out;
    }

    private BigDecimal safeBigDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private BigDecimal txAmount(InquireTransactionHistoryListREC r) {
        String raw = r.getTransactionBalance(); // 거래금액
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO;
        return safeBigDecimal(raw);
    }

    private String toStandardType(String reqType) {
        String t = (reqType == null ? "TIGGLE" : reqType).toUpperCase();
        if ("CHANGE".equals(t)) return "TIGGLE"; // 과거 요청과 호환
        if ("ALL".equals(t))   return "ALL";
        return "TIGGLE".equals(t) ? "TIGGLE" : "DUTCHPAY";
    }

    // summary + memo 둘 다 검사 (필요 시 UID 태깅 강제도 가능)
    private boolean matchTypeBySummary(SimpleTx tx, String standard, Long userId) {
        String s = tx.summary == null ? "" : tx.summary;
        String m = tx.memo    == null ? "" : tx.memo;

        boolean isTiggle = containsAny(s, "[TIGGLE]","[CHANGE]","자투리") || containsAny(m, "[TIGGLE]","[CHANGE]","자투리");
        boolean isDutch  = containsAny(s, "[DUTCH]","더치페이")            || containsAny(m, "[DUTCH]","더치페이");

        if ("ALL".equals(standard)) return isTiggle || isDutch;
        return "TIGGLE".equals(standard) ? isTiggle : isDutch;
    }

    // TIGGLE/DUTCHPAY 실제 분류(ALL 응답일 때에 사용)
    private String classifyType(SimpleTx tx) {
        String s = tx.summary == null ? "" : tx.summary;
        String m = tx.memo    == null ? "" : tx.memo;

        boolean isTiggle = containsAny(s, "[TIGGLE]","[CHANGE]","자투리") || containsAny(m, "[TIGGLE]","[CHANGE]","자투리");
        boolean isDutch  = containsAny(s, "[DUTCH]","더치페이")            || containsAny(m, "[DUTCH]","더치페이");

        if (isTiggle && !isDutch) return "TIGGLE";
        if (isDutch  && !isTiggle) return "DUTCHPAY";
        // 둘 다/둘 다 아님 → 우선 TIGGLE
        return isTiggle ? "TIGGLE" : "DUTCHPAY";
    }

    private boolean containsAny(String text, String... needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
    }

    private static class Cursor { long epochMilli; String id; }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(cursor);
            @SuppressWarnings("unchecked")
            Map<String, Object> map = MAPPER.readValue(bytes, Map.class);
            Cursor c = new Cursor();
            c.epochMilli = ((Number) map.get("t")).longValue();
            c.id = (String) map.get("i");
            return c;
        } catch (Exception e) { return null; }
    }

    private String encodeCursor(SimpleTx tx) {
        try {
            Map<String, Object> map = Map.of(
                    "t", tx.occurredAt.atZone(KST).toInstant().toEpochMilli(),
                    "i", tx.id
            );
            return Base64.getEncoder().encodeToString(MAPPER.writeValueAsBytes(map));
        } catch (Exception e) { return null; }
    }

    private boolean isBeforeCursor(SimpleTx tx, Cursor c) {
        long t = tx.occurredAt.atZone(KST).toInstant().toEpochMilli();
        if (t < c.epochMilli) return true;
        if (t > c.epochMilli) return false;
        return tx.id.compareTo(c.id) < 0;
    }

    // === 타이틀: TIGGLE만 "8월의 N번째 적립" 형식으로 표시, DUTCHPAY는 기본 라벨 ===
    private String buildMonthlyTitle(LocalDate date, String typeLabel, Integer nthInMonth) {
        if ("TIGGLE".equals(typeLabel) && nthInMonth != null && nthInMonth > 0) {
            int month = date.getMonthValue();
            return month + "월의 " + nthInMonth + "번째 적립";
        }
        // DUTCHPAY 등은 단순 라벨
        return "DUTCHPAY".equals(typeLabel) ? "더치페이 적립" : "자투리 적립";
    }

    /** TIGGLE 입금에 대해 '월별 N번째' 순번 계산 (오래된 순서부터 1,2,3…) */
    private Map<String, Integer> computeMonthlyOrdinals(List<SimpleTx> txs) {
        // 오래된 것부터 TIGGLE만 걸러서 월별 카운트를 증가
        List<SimpleTx> asc = txs.stream()
                .sorted(Comparator
                        .comparing((SimpleTx t) -> t.occurredAt)
                        .thenComparing(t -> t.id))
                .toList();

        Map<String, Integer> counters = new HashMap<>(); // key = yyyy-MM
        Map<String, Integer> ordinals = new HashMap<>(); // txId -> nth

        for (SimpleTx tx : asc) {
            String type = classifyType(tx);
            if (!"TIGGLE".equals(type)) continue; // TIGGLE만 번호 부여

            LocalDate d = tx.occurredAt.toLocalDate();
            String key = d.getYear() + "-" + String.format("%02d", d.getMonthValue());
            int next = counters.getOrDefault(key, 0) + 1;
            counters.put(key, next);
            ordinals.put(tx.id, next);
        }
        return ordinals;
    }

    private String buildWeekTitle(LocalDate date, String standardType) {
        // 더 이상 사용하지 않지만, 기존 코드 의존성이 있으면 남겨둘 수 있음.
        // 현재는 buildMonthlyTitle을 사용.
        int month = date.getMonthValue();
        LocalDate firstMon = date.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int week = (int) (ChronoUnit.WEEKS.between(firstMon, date.with(DayOfWeek.MONDAY)) + 1);
        String ko = "TIGGLE".equals(standardType) ? "자투리 적립" : "더치페이 적립";
        return month + "월의 " + week + "번째 " + ko;
    }

    private String buildWeekTitle(LocalDate date, String standardType, int nthInWeek) {
        // 미사용 (참고용)
        int month = date.getMonthValue();
        LocalDate firstMon = date.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int weekOfMonth = (int) (ChronoUnit.WEEKS.between(firstMon, date.with(DayOfWeek.MONDAY)) + 1);

        String ko = "TIGGLE".equals(standardType) ? "자투리 적립" : "더치페이 적립";
        return month + "월의 " + weekOfMonth + "번째 주 " + ko + " · " + nthInWeek + "번째";
    }

    private static class SimpleTx {
        String id;
        LocalDateTime occurredAt;
        BigDecimal amount;
        String summary;
        String memo;
        String typeCode;  // "IN" / "OUT" / "ETC"
    }

    private boolean isDeposit(InquireTransactionHistoryListREC r) {
        String t = Optional.ofNullable(r.getTransactionType()).orElse("");
        String n = Optional.ofNullable(r.getTransactionTypeName()).orElse("");
        return "D".equalsIgnoreCase(t) || "1".equals(t) || n.contains("입금");
    }

    private boolean isWithdrawal(InquireTransactionHistoryListREC r) {
        String t = Optional.ofNullable(r.getTransactionType()).orElse("");
        String n = Optional.ofNullable(r.getTransactionTypeName()).orElse("");
        return "W".equalsIgnoreCase(t) || "2".equals(t) || n.contains("출금");
    }

    // 사용자 태그 + 자투리/더치페이 태그 동시 확인
    private boolean isUserSavingRecord(InquireTransactionHistoryListREC r, Long userId) {
        final String summary = Optional.ofNullable(r.getTransactionSummary()).orElse("");
        final String memo    = Optional.ofNullable(r.getTransactionMemo()).orElse("");

        String[] userTags = new String[] {
                "[UID:" + userId + "]",
                "[UID=" + userId + "]",
                "[userId=" + userId + "]",
                "[USER:" + userId + "]"
        };
        boolean taggedWithUser = containsAny(summary, userTags) || containsAny(memo, userTags);

        boolean taggedAsSaving =
                summary.contains("[CHANGE]") || summary.contains("자투리") ||
                        summary.contains("[DUTCH]")  || summary.contains("더치페이") ||
                        summary.contains("[TIGGLE]") ||
                        memo.contains("[CHANGE]")    || memo.contains("자투리") ||
                        memo.contains("[DUTCH]")     || memo.contains("더치페이") ||
                        memo.contains("[TIGGLE]");

        return taggedWithUser && taggedAsSaving;
    }

    private String maskAccount(String acc) {
        if (acc == null || acc.isBlank()) return acc;
        int n = acc.length();
        if (n <= 4) return "****";
        return "*".repeat(Math.max(0, n - 4)) + acc.substring(n - 4);
    }
}
