package com.example.tiggle.scheduler;

import com.example.tiggle.dto.finopenapi.response.InquireTransactionHistoryListREC;
import com.example.tiggle.entity.EsgCategory; // 엔티티
import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.entity.University;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.piggybank.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeeklyUniversityDonationScheduler {

    private final PiggyBankRepository piggyBankRepository;
    private final StudentRepository usersRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    private static final long   SETTLEMENT_USER_ID    = 1L;              // 정산 유저 ID (원하면 이메일 사용)
    private static final String SETTLEMENT_USER_EMAIL = "";              // "service@tiggle.com" 등 (비워두면 ID 사용)
    private static final String LOG_DIR =
            Paths.get(System.getProperty("user.dir"), "donation-logs").toString();

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter WEEK_TAG_FMT = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd(월요일 기준)

    // 매주 일요일 19:00 KST
    @Scheduled(cron = "0 41 0 ? * SAT", zone = "Asia/Seoul")
    @Transactional
    public void runWeeklyUniversityDonation() {
        log.info("[WeeklyUniversityDonation] START (KST now={})", ZonedDateTime.now(KST));

        // 0) 정산 유저(users) 조회 → 계좌/키 확보
        Users settlementUser = resolveSettlementUser();
        if (settlementUser == null) {
            log.warn("[WeeklyUniversityDonation] cannot resolve settlement user — abort");
            return;
        }
        if (isBlank(settlementUser.getPrimaryAccountNo()) || isBlank(settlementUser.getUserKey())) {
            log.warn("[WeeklyUniversityDonation] settlement user missing primary_account_no or user_key — abort (userId={})",
                    settlementUser.getId());
            return;
        }
        final String settlementAccountNo = settlementUser.getPrimaryAccountNo();
        final String settlementUserKey;
        try {
            settlementUserKey = encryptionService.decrypt(settlementUser.getUserKey());
        } catch (Exception e) {
            log.warn("[WeeklyUniversityDonation] decrypt settlement userKey failed — abort. msg={}", e.getMessage(), e);
            return;
        }

        // 1) 이번 주 월~일 & 태그
        LocalDate today = LocalDate.now(KST);
        LocalDate weekMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        final String weekTag = "[W:" + weekMon.format(WEEK_TAG_FMT) + "]";

        // 2) 대상: autoDonation=true && current >= target
        List<PiggyBank> targets = piggyBankRepository.findAllByAutoDonationTrue().stream()
                .filter(p -> {
                    Users u = p.getOwner();
                    if (u == null || u.getUniversity() == null) return false;
                    if (p.getEsgCategory() == null) return false; // 엔티티
                    if (isBlank(p.getAccountNo())) return false;
                    BigDecimal cur = nvl(p.getCurrentAmount());
                    BigDecimal tgt = nvl(p.getTargetAmount());
                    return tgt.compareTo(BigDecimal.ZERO) > 0 && cur.compareTo(tgt) >= 0;
                })
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            log.info("[WeeklyUniversityDonation] no targets — END");
            return;
        }

        // 3) 대학별 그룹
        Map<Long, List<PiggyBank>> byUniv = targets.stream()
                .collect(Collectors.groupingBy(p -> p.getOwner().getUniversity().getId()));

        // 4) 로그 디렉터리 준비
        Path dir = Paths.get(LOG_DIR);
        try { Files.createDirectories(dir); }
        catch (IOException e) { log.warn("[WeeklyUniversityDonation] cannot create log dir {}: {}", LOG_DIR, e.getMessage(), e); return; }

        // 5) 대학별 처리
        for (Map.Entry<Long, List<PiggyBank>> entry : byUniv.entrySet()) {
            List<PiggyBank> list = entry.getValue();
            if (list.isEmpty()) continue;

            University univ = list.get(0).getOwner().getUniversity();
            String univName = Optional.ofNullable(univ.getName()).orElse("UnknownUniversity");

            String fileBase = LocalDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String safeUnivName = toSafeFileName(univName);
            Path file = dir.resolve(fileBase + "-" + univ.getId() + "-" + safeUnivName + ".log");

            Map<Integer, BigDecimal> themeSum = new HashMap<>();
            themeSum.put(1, BigDecimal.ZERO);
            themeSum.put(2, BigDecimal.ZERO);
            themeSum.put(3, BigDecimal.ZERO);

            String tsPrefix = LocalDateTime.now(KST).format(TS) + "] ";

            try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                // 헤더
                bw.write(tsPrefix + univName + " 학교 계좌로 학생들 티끌 저금통 수금 시작");
                bw.newLine();

                // 5-1) 유저별 출금 → 파일 기록 → 합산
                for (PiggyBank p : list) {
                    Users u = p.getOwner();
                    String userName = Optional.ofNullable(u.getName()).orElse("UnknownUser");
                    String piggyAcc = Optional.ofNullable(p.getAccountNo()).orElse("N/A");
                    int catId = themeId(p.getEsgCategory()); // 1/2/3 (EsgCategory.name 기반)
                    String theme = themeName(catId) + "테마";
                    BigDecimal amount = nvl(p.getTargetAmount());

                    // 사용자 userKey 복호화
                    final String userKey;
                    try {
                        if (isBlank(u.getUserKey())) {
                            bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 스킵(사유:userKey 없음)");
                            bw.newLine();
                            continue;
                        }
                        userKey = encryptionService.decrypt(u.getUserKey());
                    } catch (Exception e) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 스킵(사유:복호화 실패)");
                        bw.newLine();
                        continue;
                    }

                    // 이번 주 중복 출금 방지(저금통 계좌 거래내역 확인)
                    boolean already = false;
                    try {
                        var res = financialApiService
                                .inquireTransactionHistoryList(userKey, piggyAcc, weekMon.format(YMD), weekSun.format(YMD), "A", "ASC")
                                .block();
                        already = res != null && res.getRec() != null && res.getRec().getList() != null
                                && res.getRec().getList().stream().anyMatch(r -> isThisWeekDonationWithdrawalTx(r, u.getId(), weekTag));
                    } catch (Exception e) {
                        log.warn("[WeeklyUniversityDonation] history inquire failed userId={} acc={} msg={}", u.getId(), piggyAcc, e.getMessage());
                    }
                    already = false;
                    if (already) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 이번 주 이미 출금됨 — 스킵");
                        bw.newLine();
                        continue;
                    }

                    // 출금 요약(태그)
                    String summary = "[TIGGLE][DONATION][WD][UID:" + u.getId() + "]" + weekTag + " 주간 자동 기부 출금";

                    // 실제 출금 (저금통 계좌에서 목표 금액만큼)
                    try {
                        var wdResp = financialApiService
                                .updateDemandDepositAccountWithdrawal(userKey, piggyAcc, amount.stripTrailingZeros().toPlainString(), summary)
                                .block();

                        boolean ok = wdResp != null && wdResp.getHeader() != null && "H0000".equals(wdResp.getHeader().getResponseCode());
                        if (!ok) {
                            String msg = (wdResp == null || wdResp.getHeader() == null) ? "no response/header" : wdResp.getHeader().getResponseMessage();
                            bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 실패: " + msg);
                            bw.newLine();
                            continue;
                        }

                        // ✅ 출금 성공 시 파일 기록
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + "에서 " + amount.stripTrailingZeros().toPlainString() + " 출금");
                        bw.newLine();

                        // ✅ DB 원자적 반영 (current_amount 차감, 카운트/합계 증가)
                        int updated = piggyBankRepository.applyDonation(p.getId(), amount);
                        if (updated == 0) {
                            // 경합/상태변경으로 WHERE 조건 불충족 → 중복 차감 방지
                            log.warn("[WeeklyUniversityDonation] DB applyDonation skipped (piggyId={}, amount={})", p.getId(), amount);
                            // 파일에도 흔적 남기고 싶다면 한 줄 기록해도 됨(선택)
                            // bw.write(tsPrefix + "(주의) DB 반영 스킵: piggyId=" + p.getId() + ", amount=" + amount.stripTrailingZeros().toPlainString());
                            // bw.newLine();
                        }

                        // 합산
                        themeSum.compute(catId, (k, v) -> (v == null ? amount : v.add(amount)));

                    } catch (Exception e) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 예외: " + e.getMessage());
                        bw.newLine();
                    }
                }

                // 5-2) 합계 송금(정산계좌 → 대학 테마계좌, 각 테마 1회)
                for (Map.Entry<Integer, BigDecimal> tsEntry : themeSum.entrySet()) {
                    int catId = tsEntry.getKey();
                    BigDecimal sum = nvl(tsEntry.getValue());
                    if (sum.compareTo(BigDecimal.ZERO) <= 0) continue;

                    String toAccount = resolveThemeAccount(univ, catId);
                    if (isBlank(toAccount)) {
                        bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 미설정 — 송금 생략 (합계 " + sum.stripTrailingZeros().toPlainString() + ")");
                        bw.newLine();
                        continue;
                    }

                    String summary = "[TIGGLE][DONATION][UNIV:" + univ.getId() + "][THEME:" + themeName(catId) + "]" + weekTag + " 주간 합산 송금";
                    String memo = univName + " " + themeName(catId) + " 테마 주간 합산 송금(" + weekMon.format(YMD) + "~" + weekSun.format(YMD) + ")";

                    try {
                        var trResp = financialApiService
                                .updateDemandDepositAccountTransfer(
                                        settlementUserKey,
                                        toAccount,                                   // 입금: 대학 테마 계좌
                                        summary,                                     // 입금요약(태그)
                                        sum.stripTrailingZeros().toPlainString(),    // 금액
                                        settlementAccountNo,                         // 출금: 정산계좌
                                        memo                                         // 출금메모
                                )
                                .block();

                        boolean ok = trResp != null && trResp.getHeader() != null && "H0000".equals(trResp.getHeader().getResponseCode());
                        if (ok) {
                            bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌로 총 " + sum.stripTrailingZeros().toPlainString() + " 송금 완료");
                            bw.newLine();
                        } else {
                            String msg = (trResp == null || trResp.getHeader() == null) ? "no response/header" : trResp.getHeader().getResponseMessage();
                            bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 송금 실패: " + msg + " (합계 " + sum.stripTrailingZeros().toPlainString() + ")");
                            bw.newLine();
                        }
                    } catch (Exception e) {
                        bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 송금 예외: " + e.getMessage());
                        bw.newLine();
                    }
                }
            } catch (IOException e) {
                log.warn("[WeeklyUniversityDonation] write file failed {}: {}", file, e.getMessage(), e);
            }
        }

        log.info("[WeeklyUniversityDonation] END");
    }

    // ===== Helpers =====

    private Users resolveSettlementUser() {
        if (!isBlank(SETTLEMENT_USER_EMAIL)) {
            return usersRepository.findByEmail(SETTLEMENT_USER_EMAIL).orElse(null);
        }
        return usersRepository.findById(SETTLEMENT_USER_ID).orElse(null);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String toSafeFileName(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        n = n.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        return n;
    }

    /** EsgCategory(엔티티) → 1/2/3 (name 기준 매핑) */
    private static int themeId(EsgCategory cat) {
        if (cat == null || cat.getName() == null) return 0;
        String n = cat.getName().trim().toLowerCase();
        return switch (n) {
            case "planet" -> 1;
            case "people" -> 2;
            case "prosperity" -> 3;
            default -> 0;
        };
    }

    private static String themeName(int id) {
        return switch (id) {
            case 1 -> "Planet";
            case 2 -> "People";
            case 3 -> "Prosperity";
            default -> "Unknown";
        };
    }

    private static String resolveThemeAccount(University u, int themeId) {
        if (u == null) return null;
        return switch (themeId) {
            case 1 -> u.getPlanetAccountNo();
            case 2 -> u.getPeopleAccountNo();
            case 3 -> u.getProsperityAccountNo();
            default -> null;
        };
    }

    /** 이번 주 사용자 출금(기부 출금) 거래인지 판별 */
    private static boolean isThisWeekDonationWithdrawalTx(InquireTransactionHistoryListREC r, Long userId, String weekTag) {
        String t = Optional.ofNullable(r.getTransactionType()).orElse("");
        String n = Optional.ofNullable(r.getTransactionTypeName()).orElse("");
        boolean withdrawal = "W".equalsIgnoreCase(t) || "2".equals(t) || n.contains("출금");
        if (!withdrawal) return false;
        String s = Optional.ofNullable(r.getTransactionSummary()).orElse("");
        String m = Optional.ofNullable(r.getTransactionMemo()).orElse("");
        return (s.contains("[TIGGLE]") || m.contains("[TIGGLE]"))
                && (s.contains("[DONATION]") || m.contains("[DONATION]"))
                && (s.contains("[WD]") || m.contains("[WD]"))
                && (s.contains("[UID:" + userId + "]") || m.contains("[UID:" + userId + "]"))
                && (s.contains(weekTag) || m.contains(weekTag));
    }
}
