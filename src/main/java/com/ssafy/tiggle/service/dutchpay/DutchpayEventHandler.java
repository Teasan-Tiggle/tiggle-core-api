package com.ssafy.tiggle.service.dutchpay;

import com.ssafy.tiggle.domain.dutchpay.event.DutchpayCreatedEvent;
import com.ssafy.tiggle.repository.dutchpay.DutchpayRepository;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.ssafy.tiggle.service.account.AccountService;
import com.ssafy.tiggle.service.notification.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class DutchpayEventHandler {

    private final StudentRepository userRepo;
    private final FcmService fcmService;
    private final AccountService accountService;
    private final DutchpayRepository dutchpayRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDutchpayCreated(DutchpayCreatedEvent e) {
        final String notiTitle = e.title();
        final String notiBody = (e.message() != null && !e.message().isBlank())
                ? e.message() : "더치페이 요청이 도착했어요.";

        for (Map.Entry<Long, Long> entry : e.userShareMap().entrySet()) {
            Long userId = entry.getKey();
            Long amountToPay = entry.getValue();

            if (Objects.equals(userId, e.creatorId())) continue;

            var user = userRepo.findById(userId).orElse(null);
            if (user == null) continue;

            String token = user.getFcmToken();
            if (token == null || token.isBlank()) continue;

            Map<String, String> data = new HashMap<>();
            data.put("type", "DUTCHPAY_REQUEST");
            data.put("dutchpayId", String.valueOf(e.dutchpayId()));
            data.put("totalAmount", String.valueOf(e.totalAmount()));
            data.put("amountToPay", String.valueOf(amountToPay));
            data.put("link", "tiggle://dutchpay/" + e.dutchpayId());

            boolean ok = fcmService.sendNotificationWithData(token, notiTitle, notiBody, data);
            if (!ok) log.warn("더치페이 FCM 전송 실패 userId={}, dutchpayId={}", userId, e.dutchpayId());
        }

        try {
            Boolean payMore = dutchpayRepository.findPayMoreById(e.dutchpayId());
            long creatorOriginal = e.userShareMap().getOrDefault(e.creatorId(), 0L);

            String encryptedUserKey = e.encryptedUserKey();

            accountService.transferTiggleIfPayMore(
                            encryptedUserKey,
                            e.creatorId(),
                            e.dutchpayId(),
                            creatorOriginal,
                            Boolean.TRUE.equals(payMore)
                    )
                    .doOnError(err -> log.warn("생성자 티끌 자동저금 실패 userId={}, dutchpayId={}, msg={}",
                            e.creatorId(), e.dutchpayId(), err.getMessage()))
                    .subscribe();

        } catch (Exception ex) {
            log.warn("생성자 자동저금 트리거 중 예외 dutchpayId={}, msg={}", e.dutchpayId(), ex.getMessage());
        }
    }
}
