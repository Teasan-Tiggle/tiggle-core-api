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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.Comparator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggySummaryServiceImpl implements PiggySummaryService {

    private final PiggyBankRepository piggyBankRepository;
    private final FinancialApiService financialApiService;

    @Value("${piggy.pool.account-no}")
    private String piggyPoolAccountNo;

    @Override
    @Transactional(readOnly = true)
    public Mono<ApiResponse<PiggySummaryResponse>> getSummary(String encryptedUserKey, Integer userId) {
        return Mono.fromCallable(() ->
                piggyBankRepository.findByOwner_Id(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."))
        ).flatMap(piggy ->
                lastWeekSavedAmount(encryptedUserKey, userId)
                        .map(lastWeek -> ApiResponse.success(
                                new PiggySummaryResponse(piggy.getName(), piggy.getCurrentAmount(), lastWeek)
                        ))
        ).subscribeOn(Schedulers.boundedElastic());
    }


    /**
     * 지난주(월~일) 적립 합계.
     * - inquireTransactionHistoryList 응답 DTO를 받으면 여기서 실제 합계 계산으로 교체해줄게.
     */
    private Mono<BigDecimal> lastWeekSavedAmount(String encryptedUserKey, Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate lastWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate lastWeekEnd = lastWeekStart.with(DayOfWeek.SUNDAY);

        DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyyMMdd");
        String start = lastWeekStart.format(ymd);
        String end = lastWeekEnd.format(ymd);

        return financialApiService
                .inquireTransactionHistoryList(encryptedUserKey, piggyPoolAccountNo, start, end, "A", "ASC")
                .map(res -> {
                    return BigDecimal.ZERO;
                })
                .onErrorResume(e -> {
                    log.error("[Piggy][Summary] 지난주 적립액 조회 실패 → 0으로 대체", e);
                    return Mono.just(BigDecimal.ZERO);
                });
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

            return financialApiService.inquireTransactionHistoryList(
                            encryptedUserKey, piggy.getAccountNo(),
                            from.format(YMD), to.format(YMD),
                            "A", "ASC" // 전체, 내림차순
                    )
                    .map(res -> {
                        // 금융 응답 → 내부 표준
                        List<SimpleTx> all = mapToSimpleTxList(res);

                        // === [중요] 입금만(D) + 태그로 타입 구분 ===
                        String standard = toStandardType(req.getType()); // "CHANGE" or "DUTCHPAY"
                        List<SimpleTx> filtered = all.stream()
                                .filter(tx -> "D".equalsIgnoreCase(tx.typeCode))            // 입금만
                                .filter(tx -> matchTypeBySummary(tx, standard))             // TIGGLE/DUTCHPAY 구분
                                .sorted(Comparator
                                        .comparing((SimpleTx t) -> t.occurredAt).reversed()
                                        .thenComparing(t -> t.id, Comparator.reverseOrder()))
                                .toList();

                        // 커서 슬라이싱
                        Cursor cur = decodeCursor(req.getCursor());
                        if (cur != null) {
                            filtered = filtered.stream().filter(tx -> isBeforeCursor(tx, cur)).toList();
                        }

                        boolean hasNext = filtered.size() > size;
                        List<SimpleTx> page = filtered.stream().limit(size).toList();

                        String nextCursor = null;
                        if (hasNext) nextCursor = encodeCursor(page.get(page.size()-1));

                        ZoneOffset offset = ZoneId.systemDefault().getRules()
                                .getOffset(Instant.now());

                        List<PiggyEntryItemDto> items = page.stream().map(tx ->
                                new PiggyEntryItemDto(
                                        tx.id,
                                        standard,
                                        tx.amount, // 입금은 + 금액
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

    // ② helper들 (클래스 내부 private 메서드로)
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
            String json = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            Map<String,Object> map = new ObjectMapper().readValue(json, new TypeReference<Map<String,Object>>(){});
            Cursor c = new Cursor();
            c.epochMilli = ((Number) map.get("t")).longValue();
            c.id = (String) map.get("i");
            return c;
        } catch (Exception e) { return null; }
    }

    private String encodeCursor(SimpleTx tx) {
        long t = tx.occurredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String json = String.format("{\"t\":%d,\"i\":\"%s\"}", t, tx.id);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBeforeCursor(SimpleTx tx, Cursor c) {
        long t = tx.occurredAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        if (t < c.epochMilli) return true;
        if (t > c.epochMilli) return false;
        return tx.id.compareTo(c.id) < 0; // 동시간대 tie-break
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
