package com.example.tiggle.service.dutchpay;

import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.entity.DutchpayShare;
import com.example.tiggle.domain.dutchpay.event.DutchpayCreatedEvent;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.response.DutchpayCreatedResponse;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.dutchpay.DutchpayRepository;
import com.example.tiggle.repository.dutchpay.DutchpayShareRepository;
import com.example.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DutchpayServiceImpl implements DutchpayService {

    private final DutchpayRepository dutchpayRepo;
    private final DutchpayShareRepository shareRepo;
    private final StudentRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ApiResponse<DutchpayCreatedResponse> create(Long creatorId, CreateDutchpayRequest req) {
        Users creator = userRepo.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 없음"));

        // 참가자 로딩/검증(본인 제외)
        if (req.participantIds() == null || req.participantIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참가자를 1명 이상 선택하세요.");
        }
        List<Long> distinctIds = req.participantIds().stream().distinct().toList();
        if (distinctIds.contains(creatorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인은 참가자 목록에 포함하지 마세요.");
        }
        List<Users> participants = userRepo.findAllById(distinctIds);
        if (participants.size() != distinctIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 참가자가 포함되어 있습니다.");
        }

        // --- 분배 로직 ---
        int n = participants.size() + 1; // 본인 포함
        long total = req.totalAmount();

        Map<Long, Long> shareMap = new LinkedHashMap<>();

        if (Boolean.TRUE.equals(req.creatorPaysRemainder())) {
            // 요구사항: (total / n) 값을 "100원 단위 올림"하여 모두에게 부과
            long base = total / n;
            long perRounded = ceilTo100(base); // 100원 단위 올림
            long sum = perRounded * n;         // 실제 청구 총합
            long piggyRemainder = sum - total; // 추후 저금통 적립 예정(지금은 저장/사용 안 함)

            // 모두 perRounded 부과
            shareMap.put(creator.getId(), perRounded);
            for (Users u : participants) {
                shareMap.put(u.getId(), perRounded);
            }

            log.debug("[Dutchpay#create] creatorPaysRemainder=true, perPerson={}, sum={}, piggyRemainder={}",
                    perRounded, sum, piggyRemainder);

        } else {
            // 기존 로직 유지: 균등분배 후 잔액을 '참가자'에게만 1원씩 분배(본인 제외)
            long base = total / n;
            long remainder = total % n;

            // 기본값
            shareMap.put(creator.getId(), base);
            for (Users u : participants) shareMap.put(u.getId(), base);

            // 잔액 1원씩 참가자에게 분배(리스트 순서 기준, 본인 제외)
            for (int i = 0; i < remainder; i++) {
                Long uid = participants.get(i % participants.size()).getId();
                shareMap.put(uid, shareMap.get(uid) + 1);
            }

            log.debug("[Dutchpay#create] creatorPaysRemainder=false, base={}, remainder={}", base, remainder);
        }
        // --- 분배 로직 끝 ---

        // 저장
        Dutchpay d = new Dutchpay();
        d.setTitle(req.title());
        d.setMessage(req.message());
        d.setTotalAmount(req.totalAmount());
        d.setCreatorPaysRemainder(req.creatorPaysRemainder());
        d.setCreator(creator);
        d = dutchpayRepo.save(d);

        List<DutchpayShare> shares = new ArrayList<>();
        for (var entry : shareMap.entrySet()) {
            DutchpayShare s = new DutchpayShare();
            s.setDutchpay(d);
            s.setUser(userRepo.getReferenceById(entry.getKey()));
            s.setAmount(entry.getValue());
            s.setStatus("REQUESTED");
            shares.add(s);
        }
        shareRepo.saveAll(shares);

        // 이벤트 발행(커밋 후 FCM)
        eventPublisher.publishEvent(new DutchpayCreatedEvent(
                d.getId(), d.getTitle(), d.getMessage(),
                d.getTotalAmount(), creator.getId(), shareMap
        ));

        // 응답
        var resp = new DutchpayCreatedResponse(
                d.getId(), d.getTitle(), d.getMessage(), d.getTotalAmount(),
                shares.stream()
                        .map(s -> new DutchpayCreatedResponse.Share(
                                s.getUser().getId(), s.getAmount(), s.getStatus()))
                        .toList()
        );
        return ApiResponse.success(resp);
    }

    private long ceilTo100(long amount) {
        return ((amount + 99) / 100) * 100;
    }
}
