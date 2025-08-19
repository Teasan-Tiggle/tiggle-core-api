package com.example.tiggle.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private final MailService mailService;
    private final Map<String, VerificationInfo> verificationStorage = new ConcurrentHashMap<>();
    private static final int EXPIRATION_MINUTES = 5; // 인증 코드 유효 시간 5분

    /**
     * 인증 코드와 생성 시간을 저장하는 내부 클래스
     */
    private static class VerificationInfo {
        private final String code;
        private final LocalDateTime createdAt;

        public VerificationInfo(String code, LocalDateTime createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }

    @Override
    public void sendVerificationCode(String email) {
        String code = generateRandomCode();
        verificationStorage.put(email, new VerificationInfo(code, LocalDateTime.now()));

        String title = "[티끌] 이메일 인증 번호 안내";
        String content = "<div style='font-family: Arial, sans-serif; text-align: center; color: #333;'>"
                + "<h2 style='color: #0046FF;'>티끌 이메일 인증</h2>"
                + "<p>안녕하세요! 티끌에 오신 것을 환영합니다.</p>"
                + "<p>요청하신 인증 코드는 다음과 같습니다:</p>"
                + "<div style='margin: 20px auto; padding: 10px; font-size: 24px; font-weight: bold; background-color: #f2f2f2; border-radius: 5px; display: inline-block;'>"
                + code
                + "</div>"
                + "<p>이 코드를 5분 내에 입력해주세요.</p>"
                + "</div>";

        mailService.sendEmail(email, title, content);
    }

    @Override
    public boolean verifyCode(String email, String userCode) {
        VerificationInfo info = verificationStorage.get(email);

        // 1. 해당 이메일로 발송된 기록 없음
        if (info == null) {
            return false;
        }

        // 2. 코드 만료
        if (info.createdAt.plusMinutes(EXPIRATION_MINUTES).isBefore(LocalDateTime.now())) {
            verificationStorage.remove(email);
            return false;
        }

        // 3. 코드 일치
        if (info.code.equals(userCode)) {
            verificationStorage.remove(email);
            return true;
        }

        // 4. 코드 불일치
        return false;
    }

    /**
     * 6자리 숫자 인증 코드를 생성
     */
    private String generateRandomCode() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("인증 코드 생성 오류", e);
        }
    }
}
