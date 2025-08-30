package com.ssafy.tiggle.scheduler;

import com.ssafy.tiggle.entity.EsgCategory;
import com.ssafy.tiggle.entity.PiggyBank;
import com.ssafy.tiggle.entity.University;
import com.ssafy.tiggle.entity.Users;
import com.ssafy.tiggle.repository.piggybank.PiggyBankRepository;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.ssafy.tiggle.service.finopenapi.FinancialApiService;
import com.ssafy.tiggle.service.security.EncryptionService;
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

    private static final long   SETTLEMENT_USER_ID    = 1L;
    private static final String SETTLEMENT_USER_EMAIL = "";
    private static final String LOG_DIR =
            Paths.get(System.getProperty("user.dir"), "donation-logs").toString();

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter WEEK_TAG_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    @Scheduled(cron = "0 0 2 ? * MON", zone = "Asia/Seoul")
    @Transactional
    public void runWeeklyUniversityDonation() {
        log.info("[WeeklyUniversityDonation] START (KST now={})", ZonedDateTime.now(KST));

        // 0) 정산 유저 조회
        Users settlementUser = resolveSettlementUser();
        if (settlementUser == null
                || isBlank(settlementUser.getPrimaryAccountNo())
                || isBlank(settlementUser.getUserKey())) {
            log.warn("[WeeklyUniversityDonation] settlement user resolve failed — abort");
            return;
        }
        final String settlementAccountNo = settlementUser.getPrimaryAccountNo();
        final String settlementUserKey;
        try {
            settlementUserKey = encryptionService.decrypt(settlementUser.getUserKey());
        } catch (Exception e) {
            log.warn("[WeeklyUniversityDonation] decrypt settlement key failed: {}", e.getMessage());
            return;
        }

        // 1) 이번 주 범위 & 태그(로그/메모용)
        LocalDate today = LocalDate.now(KST);
        LocalDate weekMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekSun = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        final String weekTag = "[W:" + weekMon.format(WEEK_TAG_FMT) + "]";

        // 2) 후보 조회: donation_ready=true + auto_donation=true + current>=target
        List<PiggyBank> targets = piggyBankRepository.findAllReadyToDonate();
        if (targets.isEmpty()) {
            log.info("[WeeklyUniversityDonation] no targets — END");
            return;
        }

        // 3) 대학별 그룹
        Map<Long, List<PiggyBank>> byUniv = targets.stream()
                .collect(Collectors.groupingBy(p -> p.getOwner().getUniversity().getId()));

        // 4) 로그 디렉터리 준비
        Path dir = Paths.get(LOG_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("[WeeklyUniversityDonation] cannot create log dir {}: {}", LOG_DIR, e.getMessage());
            return;
        }

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

            String tsPrefix = "[" + LocalDateTime.now(KST).format(TS) + "] ";

            try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                bw.write(tsPrefix + univName + " 학교 계좌로 학생들 티끌 저금통 모금 시작");
                bw.newLine();

                // 5-1) 유저별 출금
                for (PiggyBank p : list) {
                    Users u = p.getOwner();
                    String userName = Optional.ofNullable(u.getName()).orElse("UnknownUser");
                    String piggyAcc = Optional.ofNullable(p.getAccountNo()).orElse("N/A");
                    int catId = themeId(p.getEsgCategory());
                    String theme = themeName(catId) + "테마";
                    BigDecimal amount = nvl(p.getTargetAmount());

                    // (A) 선점(락): donation_ready=1 -> 0 으로 바꿔서 내가 처리권 확보
                    int slot = usersRepository.acquireDonationSlot(u.getId());
                    if (slot == 0) {
                        // 이미 다른 워커/서버가 처리 중이거나 방금 처리 완료됨
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 선점 실패 — 스킵");
                        bw.newLine();
                        continue;
                    }

                    // (B) 안전 가드: 실제 잔액이 target보다 작아졌다면 스킵
                    if (nvl(p.getCurrentAmount()).compareTo(amount) < 0) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc +
                                " 현재 잔액 부족 — 스킵(current=" +
                                nvl(p.getCurrentAmount()).stripTrailingZeros().toPlainString() +
                                ", target=" + amount.stripTrailingZeros().toPlainString() + ")");
                        bw.newLine();
                        // 선점으로 donation_ready는 이미 0이므로 추가 조치 불필요
                        continue;
                    }

                    // (C) userKey 복호화
                    final String userKey;
                    try {
                        if (isBlank(u.getUserKey())) {
                            bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 스킵(사유:userKey 없음)");
                            bw.newLine();
                            // 실패이긴 하지만 조건이 아직 충족이면 다시 ready로(선택: 여기선 굳이 안 올림)
                            continue;
                        }
                        userKey = encryptionService.decrypt(u.getUserKey());
                    } catch (Exception e) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 스킵(사유:복호화 실패)");
                        bw.newLine();
                        // 복구: 조건 유지 시 다시 ready=1
                        usersRepository.markDonationReadyIfReached(u.getId());
                        continue;
                    }

                    // (D) 출금 수행 (저금통 계좌에서 target만큼)
                    String summary = "[TIGGLE][DONATION][WD][UID:" + u.getId() + "]" + weekTag + " 주간 자동 기부 출금";
                    try {
                        var wdResp = financialApiService
                                .updateDemandDepositAccountWithdrawal(userKey, piggyAcc,
                                        amount.stripTrailingZeros().toPlainString(), summary)
                                .block();

                        boolean ok = wdResp != null && wdResp.getHeader() != null
                                && "H0000".equals(wdResp.getHeader().getResponseCode());
                        if (!ok) {
                            String msg = (wdResp == null || wdResp.getHeader() == null)
                                    ? "no response/header" : wdResp.getHeader().getResponseMessage();
                            bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 실패: " + msg);
                            bw.newLine();
                            // 복구: 조건 유지 시 다시 ready=1
                            usersRepository.markDonationReadyIfReached(u.getId());
                            continue;
                        }

                        // (E) 파일 기록
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc +
                                "에서 " + amount.stripTrailingZeros().toPlainString() + "원" + " 출금");
                        bw.newLine();

                        // (F) DB 반영(원자적 차감/카운트/합계 증가)
                        int updated = piggyBankRepository.applyDonation(p.getId(), amount);
                        if (updated == 0) {
                            // 경합 등으로 실패 → 합산 제외하고 플래그 복구
                            log.warn("[WeeklyUniversityDonation] DB applyDonation skipped (piggyId={}, amount={})", p.getId(), amount);
                            usersRepository.markDonationReadyIfReached(u.getId());
                            continue;
                        }

                        // (G) 성공: 합산 (플래그는 선점에서 이미 0이라 추가 조치 없음)
                        themeSum.compute(catId, (k, v) -> (v == null ? amount : v.add(amount)));

                    } catch (Exception e) {
                        bw.write(tsPrefix + userName + "님의 " + theme + " 계좌 " + piggyAcc + " 출금 예외: " + e.getMessage());
                        bw.newLine();
                        // 복구: 조건 유지 시 다시 ready=1
                        usersRepository.markDonationReadyIfReached(u.getId());
                    }
                }

                // 5-2) 대학×테마 합산 송금
                for (Map.Entry<Integer, BigDecimal> tsEntry : themeSum.entrySet()) {
                    int catId = tsEntry.getKey();
                    BigDecimal sum = nvl(tsEntry.getValue());
                    if (sum.compareTo(BigDecimal.ZERO) <= 0) continue;

                    String toAccount = resolveThemeAccount(univ, catId);
                    if (isBlank(toAccount)) {
                        bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 미설정 — 송금 생략 (합계 " +
                                sum.stripTrailingZeros().toPlainString() + ")");
                        bw.newLine();
                        continue;
                    }

                    String summary = "[TIGGLE][DONATION][UNIV:" + univ.getId() + "][THEME:" + themeName(catId) + "]"
                            + weekTag + " 주간 합산 송금";
                    String memo = univName + " " + themeName(catId) + " 테마 주간 합산 송금("
                            + weekMon.format(YMD) + "~" + weekSun.format(YMD) + ")";
                    try {
                        var trResp = financialApiService.updateDemandDepositAccountTransfer(
                                settlementUserKey, toAccount, summary,
                                sum.stripTrailingZeros().toPlainString(),
                                settlementAccountNo, memo
                        ).block();

                        boolean ok = trResp != null && trResp.getHeader() != null
                                && "H0000".equals(trResp.getHeader().getResponseCode());
                        if (ok) {
                            bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌로 총 "
                                    + sum.stripTrailingZeros().toPlainString() + "원" + " 송금 완료");
                            bw.newLine();
                        } else {
                            String msg = (trResp == null || trResp.getHeader() == null)
                                    ? "no response/header" : trResp.getHeader().getResponseMessage();
                            bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 송금 실패: "
                                    + msg + " (합계 " + sum.stripTrailingZeros().toPlainString() + ")");
                            bw.newLine();
                        }
                    } catch (Exception e) {
                        bw.write(tsPrefix + univName + " " + themeName(catId) + " 계좌 송금 예외: " + e.getMessage());
                        bw.newLine();
                    }
                }
            } catch (IOException e) {
                log.warn("[WeeklyUniversityDonation] write file failed {}: {}", file, e.getMessage());
            }
        }

        log.info("[WeeklyUniversityDonation] END");
    }

    private Users resolveSettlementUser() {
        if (!isBlank(SETTLEMENT_USER_EMAIL))
            return usersRepository.findByEmail(SETTLEMENT_USER_EMAIL).orElse(null);
        return usersRepository.findById(SETTLEMENT_USER_ID).orElse(null);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String toSafeFileName(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        n = n.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        return n;
    }

    private static int themeId(EsgCategory cat) {
        if (cat == null || cat.getName() == null) return 0;
        String n = cat.getName().trim().toLowerCase();
        return switch (n) { case "planet" -> 1; case "people" -> 2; case "prosperity" -> 3; default -> 0; };
    }
    private static String themeName(int id) {
        return switch (id) { case 1 -> "Planet"; case 2 -> "People"; case 3 -> "Prosperity"; default -> "Unknown"; };
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
}
