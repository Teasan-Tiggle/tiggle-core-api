package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListResponse;
import com.example.tiggle.dto.piggy.request.PiggyEntriesPageRequest;
import com.example.tiggle.dto.piggy.response.PiggyEntriesPageResponse;
import com.example.tiggle.dto.piggy.response.PiggyEntryItemDto;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import com.example.tiggle.repository.piggy.PiggyBankRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggySummaryServiceImpl implements PiggySummaryService {

    private final PiggyBankRepository piggyBankRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService; // ★ 추가

    @Override
    public Mono<ApiResponse<PiggySummaryResponse>> getSummary(String encryptedUserKey, Integer userId) {
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
                            new PiggySummaryResponse(piggy.getName(), piggy.getCurrentAmount(), lastWeek)
                    ));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 지난주(월~일) 적립 합계 (입금 D 만 합산) — userKey는 평문 */
    private Mono<BigDecimal> lastWeekSavedAmount(String userKeyPlain, String accountNo, Integer userId) {
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
                            .filter(r -> "D".equalsIgnoreCase(r.getTransactionType()))
                            // .filter(r -> isUserSavingRecord(r, userId)) // 태그로 더 좁히려면 주석 해제
                            .map(r -> safeBigDecimal(r.getTransactionBalance()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                })
                .onErrorResume(e -> {
                    log.error("[Piggy][Summary] 지난주 적립액 조회 실패 → 0으로 대체 (accountNo={})", accountNo, e);
                    return Mono.just(BigDecimal.ZERO);
                });
    }

    private boolean isUserSavingRecord(InquireTransactionHistoryListREC r, Integer userId) {
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
    public Mono<ApiResponse<PiggyEntriesPageResponse>> getEntriesPage(
            String encryptedUserKey, Integer userId, PiggyEntriesPageRequest req) {

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
                                .filter(tx -> "D".equalsIgnoreCase(tx.typeCode)) // 입금만
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

                        List<PiggyEntryItemDto> items = page.stream().map(tx ->
                                new PiggyEntryItemDto(
                                        tx.id,
                                        standard,
                                        tx.amount,
                                        tx.occurredAt.atOffset(offset),
                                        buildWeekTitle(tx.occurredAt.toLocalDate(), standard)
                                )
                        ).toList();

                        return ApiResponse.success(new PiggyEntriesPageResponse(items, nextCursor, items.size(), hasNext));
                    })
                    .onErrorResume(e -> {
                        log.error("[Piggy][EntriesPage] 조회 실패", e);
                        return Mono.just(ApiResponse.success(new PiggyEntriesPageResponse(List.of(), null, 0, false)));
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
            if ("W".equalsIgnoreCase(r.getTransactionType())) amount = amount.negate();

            SimpleTx tx = new SimpleTx();
            tx.id = r.getTransactionUniqueNo();
            tx.occurredAt = occurredAt;
            tx.amount = amount;
            tx.summary = r.getTransactionSummary();
            tx.typeCode = r.getTransactionType();
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
        return "TIGGLE".equals(t) ? "CHANGE" : "DUTCHPAY";
    }

    private boolean matchTypeBySummary(SimpleTx tx, String standard) {
        String s = tx.summary == null ? "" : tx.summary;
        boolean isChange = s.contains("[CHANGE]") || s.contains("자투리");
        boolean isDutch  = s.contains("[DUTCH]")  || s.contains("더치페이");
        return "CHANGE".equals(standard) ? isChange : isDutch;
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
        String ko = "CHANGE".equals(standardType) ? "자투리 적립" : "더치페이 적립";
        return month + "월의 " + week + "번째 " + ko;
    }

    private static class SimpleTx {
        String id;
        LocalDateTime occurredAt;
        BigDecimal amount;
        String summary;
        String typeCode; // "D"/"W"
    }
}
