package com.example.tiggle.service.dutchpay;

import com.example.tiggle.domain.dutchpay.event.DutchpayCreatedEvent;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.entity.DutchpayShare;
import com.example.tiggle.entity.DutchpayShareStatus;
import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.dutchpay.DutchpayRepository;
import com.example.tiggle.repository.dutchpay.DutchpayShareRepository;
import com.example.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DutchpayServiceImpl implements DutchpayService {

    private final DutchpayRepository dutchpayRepo;
    private final DutchpayShareRepository shareRepo;
    private final StudentRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;

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
            shareMap.put(creator.getId(), shareMap.get(creator.getId()) + remainder);
        } else {
            for (int i = 0; i < remainder; i++) {
                Long uid = participants.get(i % participants.size()).getId();
                shareMap.put(uid, shareMap.get(uid) + 1);
            }
        }

        // 저장
        Dutchpay d = new Dutchpay();
        d.setTitle(req.title());
        d.setMessage(req.message());
        d.setTotalAmount(req.totalAmount());
        d.setPayMore(req.payMore());
        d.setCreator(creator);
        d = dutchpayRepo.save(d);

        List<DutchpayShare> shares = new ArrayList<>();
        for (var entry : shareMap.entrySet()) {
            Long uid = entry.getKey();
            long amt = entry.getValue();

            DutchpayShare s = new DutchpayShare();
            s.setDutchpay(d);
            s.setUser(userRepo.getReferenceById(uid));
            s.setAmount(amt);

            s.setStatus(uid.equals(creator.getId())
                    ? DutchpayShareStatus.PAID
                    : DutchpayShareStatus.PENDING);

            shares.add(s);
        }
        shareRepo.saveAll(shares);

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
}
