package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import com.example.tiggle.repository.piggy.PiggyBankRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
                .inquireTransactionHistoryList(encryptedUserKey, piggyPoolAccountNo, start, end, "A", "D")
                .map(res -> {
                    // TODO: res에서 우리 규칙(예: summary에 "[PIGGY][DEPOSIT][userId=123]")으로 필터링 후 금액 합계
                    return BigDecimal.ZERO;
                })
                .onErrorResume(e -> {
                    log.error("[Piggy][Summary] 지난주 적립액 조회 실패 → 0으로 대체", e);
                    return Mono.just(BigDecimal.ZERO);
                });
    }
}
