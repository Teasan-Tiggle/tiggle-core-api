package com.example.tiggle.service.dutchpay;

import com.example.tiggle.domain.dutchpay.event.DutchpayCreatedEvent;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.example.tiggle.dto.dutchpay.response.DutchpayDetailResponse;
import com.example.tiggle.dto.dutchpay.response.DutchpayListItemResponse;
import com.example.tiggle.dto.dutchpay.response.DutchpayListResponse;
import com.example.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;
import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.entity.DutchpayShare;
import com.example.tiggle.entity.DutchpayShareStatus;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.dutchpay.DutchpayQueryRepository;
import com.example.tiggle.repository.dutchpay.DutchpayRepository;
import com.example.tiggle.repository.dutchpay.DutchpayShareRepository;
import com.example.tiggle.repository.dutchpay.projection.DutchpayListItemProjection;
import com.example.tiggle.repository.piggybank.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DutchpayServiceImpl implements DutchpayService {

    private final DutchpayRepository dutchpayRepo;
    private final DutchpayShareRepository shareRepo;
    private final StudentRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final DutchpayQueryRepository queryRepo;

    // Summary 용 의존성
    private final PiggyBankRepository piggyBankRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    /* ================== CREATE ================== */
    @Override
    @Transactional
    public void create(String encryptedUserKey, Long creatorId, CreateDutchpayRequest req) {
        Users creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        // 기본 검증
        if (req.userIds() == null || req.userIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참가자를 1명 이상 선택하세요.");
        }
        if (req.totalAmount() == null || req.totalAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "총 금액이 올바르지 않습니다.");
        }

        // 참가자 로딩/검증(본인 제외)
        List<Long> distinctIds = req.userIds().stream().distinct().toList();
        if (distinctIds.contains(creatorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인은 참가자 목록에 포함하지 마세요.");
        }
        List<Users> participants = userRepo.findAllById(distinctIds);
        if (participants.size() != distinctIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 참가자가 포함되어 있습니다.");
        }

        // 동일 분배: base + remainder
        int n = participants.size() + 1; // 본인 포함
        long base = req.totalAmount() / n;
        long remainder = req.totalAmount() % n;

        Map<Long, Long> shareMap = new LinkedHashMap<>();
        shareMap.put(creator.getId(), base);
        for (Users u : participants) shareMap.put(u.getId(), base);

        if (Boolean.TRUE.equals(req.payMore())) {
            // 생성자가 '티끌 더하기' 선택 시 잔여분 생성자에게
            shareMap.put(creator.getId(), shareMap.get(creator.getId()) + remainder);
        } else {
            // 아니면 잔여 1원씩 참가자에게 라운드 로빈 분배
            for (int i = 0; i < remainder; i++) {
                Long uid = participants.get(i % participants.size()).getId();
                shareMap.put(uid, shareMap.get(uid) + 1);
            }
        }

        // 더치페이 저장
        Dutchpay d = new Dutchpay();
        d.setTitle(req.title());
        d.setMessage(req.message());
        d.setTotalAmount(req.totalAmount());
        d.setPayMore(req.payMore());
        d.setCreator(creator);
        d = dutchpayRepo.save(d);

        // 지분 저장 (생성자는 PAID, 나머지는 PENDING)
        List<DutchpayShare> shares = new ArrayList<>();
        for (var entry : shareMap.entrySet()) {
            Long uid = entry.getKey();
            long amt = entry.getValue();

            DutchpayShare s = new DutchpayShare();
            s.setDutchpay(d);
            s.setUser(userRepo.getReferenceById(uid));
            s.setAmount(amt);

            if (uid.equals(creator.getId())) {
                s.setStatus(DutchpayShareStatus.PAID);
                s.setPayMore(false);
                s.setTiggleAmount(0L);
                s.setPaidAmount(amt);
                s.setNotifiedAt(java.time.LocalDateTime.now());
            } else {
                s.setStatus(DutchpayShareStatus.PENDING);
            }
            shares.add(s);
        }
        shareRepo.saveAll(shares);

        // 이벤트 발행 (푸시/자동저금 등 후속 작업)
        eventPublisher.publishEvent(new DutchpayCreatedEvent(
                d.getId(),
                d.getTitle(),
                d.getMessage(),
                d.getTotalAmount(),
                creator.getId(),
                shareMap,
                encryptedUserKey
        ));
    }

    /* ================== DETAIL ================== */
    @Override
    @Transactional(readOnly = true)
    public DutchpayDetailResponse  getDetail(Long dutchpayId, Long userId) {
        // 1) 헤더 로드
        var h = queryRepo.findDetailHeader(dutchpayId);
        if (h == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "더치페이를 찾을 수 없습니다.");
        }

        // 2) 참여자 목록 로드
        var rows = queryRepo.findDetailShares(dutchpayId);
        if (rows == null || rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "참여자 정보가 없습니다.");
        }

        // 3) 권한 체크: 생성자이거나, shares에 내가 포함되어 있어야 함
        boolean isCreator = h.getCreatorId() != null && h.getCreatorId().equals(userId);
        boolean isParticipant = rows.stream().anyMatch(r -> r.getUserId().equals(userId));
        if (!isCreator && !isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 더치페이에 접근 권한이 없습니다.");
        }

        // 4) 매핑
        var shares = rows.stream()
                .map(r -> new com.example.tiggle.dto.dutchpay.response.DutchpayDetailResponse.Share(
                        r.getUserId(),
                        r.getUserName(),
                        r.getAmount(),
                        r.getStatus()
                ))
                .toList();

        return new DutchpayDetailResponse(
                h.getDutchpayId(),
                h.getTitle(),
                h.getMessage(),
                h.getTotalAmount(),
                h.getStatus(),
                new com.example.tiggle.dto.dutchpay.response.DutchpayDetailResponse.Creator(
                        h.getCreatorId(),
                        h.getCreatorName()
                ),
                shares,
                h.getRoundedPerPerson(),
                h.getPayMore(),
                h.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public DutchpayListResponse getDutchpayListCursor(Long userId, String tab, String cursor, Integer size) {
        int pageSize = (size == null || size <= 0) ? 20 : size;
        int limit = pageSize + 1; // hasNext 판별용

        Cur cur = parseCursor(cursor);
        boolean completed = "COMPLETED".equalsIgnoreCase(tab) || "DONE".equalsIgnoreCase(tab) || "완료".equals(tab);

        List<DutchpayListItemProjection> rows = completed
                ? queryRepo.findCompletedAfter(userId, cur.at, cur.id, limit)
                : queryRepo.findInProgressAfter(userId, cur.at, cur.id, limit);

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) rows = rows.subList(0, pageSize);

        String nextCursor = null;
        if (!rows.isEmpty()) {
            var last = rows.get(rows.size() - 1);
            nextCursor = toCursor(last.getRequestedAt(), last.getDutchpayId());
        }

        var items = rows.stream().map(r -> {
            long myAmt = nvl(r.getMyAmount());

            boolean paid     = "PAID".equalsIgnoreCase(r.getMyStatus());
            boolean payMore  = nvlI(r.getMyPayMore()) == 1;
            long tiggleSaved = nvl(r.getMyTiggleAmount());
            long tiggle      = (paid && payMore) ? tiggleSaved : 0L;

            return DutchpayListItemResponse.builder()
                    .dutchpayId(r.getDutchpayId())
                    .title(r.getTitle())
                    .myAmount(myAmt)
                    .totalAmount(nvl(r.getTotalAmount()))
                    .participantCount(nvlI(r.getParticipantCount()))
                    .paidCount(nvlI(r.getPaidCount()))
                    .requestedAt(r.getRequestedAt())
                    .isCreator(nvlI(r.getIsCreator()) == 1)
                    .creatorName(r.getCreatorName())
                    .tiggleAmount(tiggle)
                    .build();
        }).toList();


        return DutchpayListResponse.builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DutchpaySummaryResponse getSummary(String encryptedUserKey, Long userId) {
        // 1) 계정/계좌 확인
        userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));
        var piggy = piggyBankRepository.findByOwner_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "저금통 계좌 없음"));

        // 2) 기본 기간: 최근 6개월
        var end   = LocalDate.now();
        var start = end.minusMonths(6);
        var FMT   = DateTimeFormatter.ofPattern("yyyyMMdd");
        String startYmd = start.format(FMT);
        String endYmd   = end.format(FMT);

        String userKey = encryptionService.decrypt(encryptedUserKey);
        String piggyAcc = normalizeAcc(piggy.getAccountNo());

        long total = 0L;
        long count = 0L;

        try {
            var resp = financialApiService
                    .inquireTransactionHistoryList(userKey, piggyAcc, startYmd, endYmd, "A", "DESC")
                    .block();

            if (resp != null && resp.getHeader() != null && "H0000".equals(resp.getHeader().getResponseCode())) {
                for (var tx : resp.getRec().getList()) {
                    String typeName  = tx.getTransactionTypeName();
                    String summary   = tx.getTransactionSummary();
                    String amountStr = tx.getTransactionBalance();

                    if (!isDeposit(typeName)) continue;
                    if (summary == null || !summary.startsWith(TIGGLE_MEMO_PREFIX)) continue;

                    long amt = parseAmount(amountStr);
                    if (amt > 0) { total += amt; count++; }
                }
            }
        } catch (Exception e) {
            log.warn("summary piggy api failed", e);
        }

        long participated = shareRepo.countDistinctDutchpayByUser(userId);
        var statusCnt = queryRepo.countStatusByUserId(userId);
        long inProgress = (statusCnt == null || statusCnt.getInProgressCount() == null) ? 0L : statusCnt.getInProgressCount();
        long completed  = (statusCnt == null || statusCnt.getCompletedCount() == null)  ? 0L : statusCnt.getCompletedCount();

        return DutchpaySummaryResponse.builder()
                .totalTransferredAmount(total)
                .transferCount(count)
                .participatedCount(participated)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .build();
    }


    private static final String TIGGLE_MEMO_PREFIX = "[TIGGLE][PM]";

    private static long nvl(Long v) { return v == null ? 0L : v; }
    private static int nvlI(Integer v) { return v == null ? 0 : v; }

    private static record Cur(LocalDateTime at, Long id) {}
    private static Cur parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return new Cur(null, null);
        try {
            String[] parts = cursor.split("\\|");
            LocalDateTime at = LocalDateTime.parse(parts[0]); // ISO 형식
            Long id = (parts.length > 1) ? Long.parseLong(parts[1]) : null;
            return new Cur(at, id);
        } catch (Exception e) {
            return new Cur(null, null);
        }
    }
    private static String toCursor(LocalDateTime at, Long id) {
        if (at == null || id == null) return null;
        return at.toString() + "|" + id; // e.g. 2025-08-27T00:15:12.123456|42
    }

    private long roundUpTo100(long amount) {
        if (amount <= 0) return 0;
        return ((amount + 99) / 100) * 100;
    }

    private static boolean isDeposit(String typeName) {
        if (typeName == null) return false;
        String t = typeName.toLowerCase();
        return t.contains("입금") || t.contains("deposit") || t.contains("credit");
    }

    private static long parseAmount(String s) {
        try { return new java.math.BigDecimal(s).longValue(); }
        catch (Exception e) { return 0L; }
    }

    private static String normalizeAcc(String acc) {
        return acc == null ? null : acc.replaceAll("\\D", "");
    }
}
