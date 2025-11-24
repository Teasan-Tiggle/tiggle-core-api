package com.ssafy.tiggle.service.sms;

import com.ssafy.tiggle.config.SmsProps;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
public class SmsService {
    private final DefaultMessageService sms;
    private final SmsProps props;
    private final OtpStore store;
    private static final SecureRandom RND = new SecureRandom();

    @Value("${features.sms.enabled:false}")
    private boolean smsEnabled;

    public SmsService(DefaultMessageService sms, SmsProps props, OtpStore store) {
        this.sms = sms; this.props = props; this.store = store;
    }

    @PostConstruct
    public void init() {
        if (smsEnabled) {
            log.info("SMS API 초기화 완료");
        } else {
            log.info("SMS API 비활성화 (features.sms.enabled=false)");
        }
    }

    private String normalizeKr(String phone) {
        String p = phone.replaceAll("\\D", "");
        if (p.startsWith("82")) p = "0" + p.substring(2);
        return p;
    }
    private String code6() { return String.valueOf(RND.nextInt(900_000) + 100_000); }

    public void sendCode(String rawPhone, String purpose, String ip) {
        if (rawPhone == null || rawPhone.isBlank()) throw new IllegalArgumentException("전화번호는 필수입니다.");
        String phone = normalizeKr(rawPhone);
        if (purpose == null || purpose.isBlank()) purpose = "account_opening";

        if (!smsEnabled) {
            log.info("[SMS] MOCK 모드 - 인증코드 발송 스킵 (phone={}, purpose={})", phone, purpose);
            return;
        }

        // 제한
        store.checkRateLimit(ip);
        store.ensureResendCooldown(purpose, phone, Duration.ofSeconds(props.resendIntervalSeconds()));

        // OTP 생성 & 저장(해시) + TTL
        String code = code6();
        store.saveOtp(purpose, phone, code, Duration.ofSeconds(props.ttlSeconds()));

        // 발송
        String text = "[티끌] 인증번호 " + code + " (유효 " + props.ttlSeconds()/60 + "분)";

        Message msg = new Message();
        msg.setFrom(props.from());
        msg.setTo(phone);
        msg.setText(text);
        SingleMessageSendingRequest req = new SingleMessageSendingRequest(msg);
        SingleMessageSentResponse res = sms.sendOne(req);
        log.info("SMS 발송 완료 - messageId: {}", res.getMessageId());
    }

    public boolean verify(String rawPhone, String purpose, String code) {
        String phone = normalizeKr(rawPhone);
        if (purpose == null || purpose.isBlank()) purpose = "account_opening";

        if (!smsEnabled) {
            log.info("[SMS] MOCK 모드 - 인증 자동 성공 처리 (phone={}, purpose={})", phone, purpose);
            return true;
        }

        return store.verifyAndConsume(purpose, phone, code, props.maxAttempts());
    }
}
