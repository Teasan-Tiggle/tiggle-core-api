package com.example.tiggle.scheduler;

import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.piggybank.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.piggybank.PiggyBankWriterService;
import com.example.tiggle.service.security.EncryptionService;
import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyAutoSavingScheduler {

    private final PiggyBankRepository piggyBankRepository;
    private final StudentRepository studentRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;
    private final PiggyBankWriterService piggyBankWriterService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter WEEK_TAG_FMT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd (월요일)

    @Scheduled(cron = "0 0 18 ? * SUN", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void runWeeklyChangeSweep() {
        log.info("[WeeklyAutoSaving] START (KST now={})", ZonedDateTime.now(KST));

        // auto_saving = 1 인 저금통만
        List<PiggyBank> targets = piggyBankRepository.findAllByAutoSavingTrue();
        if (targets.isEmpty()) {
            log.info("[WeeklyAutoSaving] no targets — skip");
            return;
        }

        // 순차 처리(안전), 필요 시 병렬조정 가능
        for (PiggyBank piggy : targets) {
            try {
                processOnePiggy(piggy)
                        .subscribeOn(Schedulers.boundedElastic())
                        .block(); // 스케줄러 한 사이클 내에서 수행
            } catch (Exception e) {
                log.warn("[WeeklyAutoSaving] process error (piggyId={}, ownerId={}) : {}",
                        piggy.getId(),
                        Optional.ofNullable(piggy.getOwner()).map(Users::getId).orElse(null),
                        e.toString(), e);
            }
        }

        log.info("[WeeklyAutoSaving] END");
    }

    private Mono<Void> processOnePiggy(PiggyBank piggy) {
        Users owner = piggy.getOwner();
        if (owner == null) {
            log.warn("[WeeklyAutoSaving] owner null (piggyId={}) — skip", piggy.getId());
            return Mono.empty();
        }
        String primary = owner.getPrimaryAccountNo();
        String piggyAcc = piggy.getAccountNo();

        if (primary == null || primary.isBlank()) {
            log.info("[WeeklyAutoSaving] primary account missing — skip userId={}", owner.getId());
            return Mono.empty();
        }
        if (piggyAcc == null || piggyAcc.isBlank()) {
            log.info("[WeeklyAutoSaving] piggy account missing — skip userId={}", owner.getId());
            return Mono.empty();
        }

        String userKey = resolveUserKey(owner);
        if (userKey == null) {
            log.info("[WeeklyAutoSaving] userKey missing — skip userId={}", owner.getId());
            return Mono.empty();
        }

        // 이번 주 월요일~일요일
        LocalDate now = LocalDate.now(KST);
        LocalDate weekMon = now.with(DayOfWeek.MONDAY);
        LocalDate weekSun = weekMon.with(DayOfWeek.SUNDAY);

        final String weekTag = "[W:" + weekMon.format(WEEK_TAG_FMT) + "]"; // [W:20250818]

        // 1) 아이템포턴시: 이번 주에 이미 실행된 적 있는지 저금통 계좌 입금내역으로 확인
        return financialApiService
                .inquireTransactionHistoryList(userKey, piggyAcc, weekMon.format(YMD), weekSun.format(YMD), "A", "ASC")
                .defaultIfEmpty(null)
                .flatMap(res -> {
                    boolean already = res != null
                            && res.getRec() != null
                            && res.getRec().getList() != null
                            && res.getRec().getList().stream().anyMatch(r -> isWeeklyAutoSavingTx(r, owner.getId(), weekTag));

                    if (already) {
                        log.info("[WeeklyAutoSaving] already done this week — skip userId={}, weekTag={}", owner.getId(), weekTag);
                        return Mono.empty();
                    }

                    // 2) 주계좌 잔액 조회 → 잔액 % 1000 계산
                    return financialApiService.inquireDemandDepositAccount(userKey, primary)
                            .flatMap(acc -> {
                                if (acc.getHeader() == null || !"H0000".equals(acc.getHeader().getResponseCode())) {
                                    String msg = acc.getHeader() == null ? "no header" : acc.getHeader().getResponseMessage();
                                    return Mono.error(new IllegalStateException("primary balance inquire failed: " + msg));
                                }
                                String balStr = Optional.ofNullable(acc.getRec().getAccountBalance()).orElse("0");
                                long balance;
                                try { balance = new BigDecimal(balStr).longValue(); } catch (Exception e) { balance = 0L; }

                                long amount = balance % 1000L;
                                if (amount <= 0) {
                                    log.info("[WeeklyAutoSaving] nothing to sweep (balance={}, remainder={}) userId={}",
                                            balance, amount, owner.getId());
                                    return Mono.empty();
                                }

                                String summary = "[TIGGLE][CHANGE][UID:" + owner.getId() + "]" + weekTag + " 주간 잔돈 자동저축";
                                String memo    = "주간 잔돈 자동저축 " + weekMon.getMonthValue() + "월 " + weekMon.getDayOfMonth() + "일~";

                                // 3) 이체 실행(출금: 주계좌 → 입금: 저금통)
                                return financialApiService.updateDemandDepositAccountTransfer(
                                                userKey,
                                                piggyAcc,                 // 입금계좌(저금통)
                                                summary,                  // 입금요약(태그 포함)
                                                String.valueOf(amount),   // 금액
                                                primary,                  // 출금계좌(주계좌)
                                                memo                      // 출금메모
                                        )
                                        .flatMap(resp -> {
                                            boolean ok = resp.getHeader() != null && "H0000".equals(resp.getHeader().getResponseCode());
                                            if (!ok) {
                                                String msg = resp.getHeader() == null ? "no header" : resp.getHeader().getResponseMessage();
                                                return Mono.error(new IllegalStateException("transfer failed: " + msg));
                                            }

                                            // 4) 저금통 반영
                                            return Mono.fromCallable(() -> {
                                                piggyBankWriterService.applyTiggle(owner.getId(), BigDecimal.valueOf(amount));
                                                return true;
                                            }).subscribeOn(Schedulers.boundedElastic()).then();
                                        });
                            });
                })
                .onErrorResume(e -> {
                    log.warn("[WeeklyAutoSaving] failed userId={}, msg={}", owner.getId(), e.getMessage(), e);
                    return Mono.empty(); // 한 유저 실패해도 전체 스케줄은 계속
                });
    }

    private String resolveUserKey(Users owner) {
        String k = owner.getUserKey();
        if (k == null || k.isBlank()) return null;
        try { return encryptionService.decrypt(k); }
        catch (Exception ignored) { return k; } // 이미 평문일 수도 있음
    }

    /** 이번 주 자동저축 이체인지(아이템포턴시 체크) */
    private boolean isWeeklyAutoSavingTx(InquireTransactionHistoryListREC r, Long userId, String weekTag) {
        // 입금만 대상
        String t = Optional.ofNullable(r.getTransactionType()).orElse("");
        String n = Optional.ofNullable(r.getTransactionTypeName()).orElse("");
        boolean deposit = "D".equalsIgnoreCase(t) || "1".equals(t) || n.contains("입금");
        if (!deposit) return false;

        String s = Optional.ofNullable(r.getTransactionSummary()).orElse("");
        String m = Optional.ofNullable(r.getTransactionMemo()).orElse("");

        return (s.contains("[TIGGLE]") || m.contains("[TIGGLE]"))
                && (s.contains("[CHANGE]") || m.contains("[CHANGE]") || s.contains("자투리") || m.contains("자투리"))
                && (s.contains("[UID:" + userId + "]") || m.contains("[UID:" + userId + "]"))
                && (s.contains(weekTag) || m.contains(weekTag));
    }
}
