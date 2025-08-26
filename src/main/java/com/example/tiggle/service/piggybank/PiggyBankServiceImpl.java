package com.example.tiggle.service.piggybank;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListResponse;
import com.example.tiggle.dto.piggybank.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggybank.request.PiggyBankEntriesPageRequest;
import com.example.tiggle.dto.piggybank.request.UpdatePiggyBankSettingsRequest;
import com.example.tiggle.dto.piggybank.response.*;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.piggybank.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggyBankServiceImpl implements PiggyBankService {

    private final PiggyBankRepository piggyBankRepository;
    private final EsgCategoryRepository esgCategoryRepository;
    private final StudentRepository studentRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

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

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
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

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Long userId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            piggy.setEsgCategory(null);
            piggyBankRepository.save(piggy);

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
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
        return Mono.fromCallable(() -> {
            // 1) 중복 방지
            piggyBankRepository.findByOwner_Id(userId).ifPresent(pb -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 개설된 저금통이 있습니다.");
            });

            // 2) 사용자 확인
            Users owner = studentRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."));

            // 3) 금융 API: 예금계좌 생성
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
            log.info("[Piggy][Create] userId={}, createdAccountNo={}", userId, createdAccountNo);

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

    /** 지난주(월~일) 적립 합계 (입금 D 만 합산) — userKey는 평문 */
    private Mono<BigDecimal> lastWeekSavedAmount(String userKeyPlain, String accountNo, Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate lastWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekEnd   = lastWeekStart.with(DayOfWeek.SUNDAY);

        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
        String start = lastWeekStart.format(ymd);
        String end   = lastWeekEnd.format(ymd);

        return financialApiService
                .inquireTransactionHistoryList(userKeyPlain, accountNo, start, end, "A", "ASC")
                .map(res -> {
                    if (res == null || res.getRec() == null || res.getRec().getList() == null) {
                        return BigDecimal.ZERO;
                    }
                    return res.getRec().getList().stream()
                            .filter(this::isDeposit)
                            .map(r -> safeBigDecimal(r.getTransactionBalance()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .onErrorResume(e -> {
                    log.error("[Piggy][Summary] 지난주 적립액 조회 실패 → 0으로 대체 (accountNo={})", accountNo, e);
                    return Mono.just(BigDecimal.ZERO);
                });
    }

    private boolean isUserSavingRecord(InquireTransactionHistoryListREC r, Long userId) {
        final String summary = r.getTransactionSummary() == null ? "" : r.getTransactionSummary();
        final String memo    = r.getTransactionMemo() == null ? "" : r.getTransactionMemo();

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
                        summary.contains("[PIGGY]");

        return taggedWithUser && taggedAsSaving;
    }

    private boolean containsAny(String text, String[] needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
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

            LocalDate from = req.getFrom() != null ? LocalDate.parse(req.getFrom()) : LocalDate.now().minusDays(30);
            LocalDate to   = req.getTo()   != null ? LocalDate.parse(req.getTo())   : LocalDate.now();
            DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
            int size = Math.min(Math.max(req.getSize() == null ? 20 : req.getSize(), 1), 100);

            final String userKey = encryptionService.decrypt(encryptedUserKey);

            return financialApiService.inquireTransactionHistoryList(
                            userKey, piggy.getAccountNo(),
                            from.format(YMD), to.format(YMD),
                            "A", "ASC"
                    )
                    .map(res -> {
                        List<SimpleTx> all = mapToSimpleTxList(res);

                        String standard = toStandardType(req.getType()); // "CHANGE" or "DUTCHPAY"
                        List<SimpleTx> filtered = all.stream()
                                .filter(tx -> "IN".equals(tx.typeCode))
                                .filter(tx -> matchTypeBySummary(tx, standard))
                                .sorted(Comparator
                                        .comparing((SimpleTx t) -> t.occurredAt).reversed()
                                        .thenComparing(t -> t.id, Comparator.reverseOrder()))
                                .toList();

                        Cursor cur = decodeCursor(req.getCursor());
                        if (cur != null) filtered = filtered.stream().filter(tx -> isBeforeCursor(tx, cur)).toList();

                        boolean hasNext = filtered.size() > size;
                        List<SimpleTx> page = filtered.stream().limit(size).toList();

                        String nextCursor = null;
                        if (hasNext) nextCursor = encodeCursor(page.get(page.size() - 1));

                        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());

                        List<PiggyBankEntryItemDto> items = page.stream().map(tx ->
                                new PiggyBankEntryItemDto(
                                        tx.id,
                                        standard,
                                        tx.amount,
                                        tx.occurredAt.atOffset(offset),
                                        buildWeekTitle(tx.occurredAt.toLocalDate(), standard)
                                )
                        ).toList();

                        return ApiResponse.success(new PiggyBankEntriesPageResponse(items, nextCursor, items.size(), hasNext));
                    })
                    .onErrorResume(e -> {
                        log.error("[Piggy][EntriesPage] 조회 실패", e);
                        return Mono.just(ApiResponse.success(new PiggyBankEntriesPageResponse(List.of(), null, 0, false)));
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static final DateTimeFormatter YMD_NUM = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HMS_NUM = DateTimeFormatter.ofPattern("HHmmss");

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

            BigDecimal amount = safeBigDecimal(r.getTransactionBalance());
            if (isWithdrawal(r) && amount.signum() > 0) amount = amount.negate();

            SimpleTx tx = new SimpleTx();
            tx.id         = r.getTransactionUniqueNo();
            tx.occurredAt = occurredAt;
            tx.amount     = amount;
            tx.summary    = Optional.ofNullable(r.getTransactionSummary()).orElse("");
            tx.typeCode   = isDeposit(r) ? "IN" : (isWithdrawal(r) ? "OUT" : Optional.ofNullable(r.getTransactionType()).orElse(""));
            out.add(tx);
        }
        return out;
    }


    private BigDecimal safeBigDecimal(String s) {
        if (s == null || s.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String toStandardType(String reqType) {
        String t = (reqType == null ? "TIGGLE" : reqType).toUpperCase();
        if ("CHANGE".equals(t)) return "TIGGLE";  // ← 과거 요청과 호환
        if ("ALL".equals(t))   return "ALL";      // (선택) 둘 다 보고 싶을 때
        return "TIGGLE".equals(t) ? "TIGGLE" : "DUTCHPAY";
    }


    private boolean matchTypeBySummary(SimpleTx tx, String standard) {
        String s = tx.summary == null ? "" : tx.summary;
        boolean isTiggle = s.contains("[TIGGLE]") || s.contains("[CHANGE]") || s.contains("자투리");
        boolean isDutch  = s.contains("[DUTCH]")  || s.contains("더치페이");

        if ("ALL".equals(standard)) return isTiggle || isDutch;
        return "TIGGLE".equals(standard) ? isTiggle : isDutch;
    }


    private static class Cursor { long epochMilli; String id; }

    private Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String json = new String(Base64.getDecoder().decode(cursor));
            var map = new HashMap<String, Object>();
            json = json.replaceAll("[\\{\\}\"]", "");
            for (String p : json.split(",")) {
                String[] kv = p.split(":");
                if (kv.length == 2) map.put(kv[0].trim(), kv[1].trim());
            }
            Cursor c = new Cursor();
            c.epochMilli = Long.parseLong((String) map.get("t"));
            c.id = (String) map.get("i");
            return c;
        } catch (Exception e) { return null; }
    }

    private String encodeCursor(SimpleTx tx) {
        long t = tx.occurredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String json = String.format("{\"t\":%d,\"i\":\"%s\"}", t, tx.id);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }

    private boolean isBeforeCursor(SimpleTx tx, Cursor c) {
        long t = tx.occurredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (t < c.epochMilli) return true;
        if (t > c.epochMilli) return false;
        return tx.id.compareTo(c.id) < 0;
    }

    private String buildWeekTitle(LocalDate date, String standardType) {
        int month = date.getMonthValue();
        LocalDate firstMon = date.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int week = (int) (ChronoUnit.WEEKS.between(firstMon, date.with(DayOfWeek.MONDAY)) + 1);
        String ko = "TIGGLE".equals(standardType) ? "자투리 적립" : "더치페이 적립";
        return month + "월의 " + week + "번째 " + ko;
    }

    private static class SimpleTx {
        String id;
        LocalDateTime occurredAt;
        BigDecimal amount;
        String summary;
        String typeCode; // "D"/"W"
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

}
