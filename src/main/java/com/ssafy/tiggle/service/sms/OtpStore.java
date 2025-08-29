package com.ssafy.tiggle.service.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
class OtpStore {
    private final StringRedisTemplate redis;

    private String otpKey(String purpose, String phone) { return "otp:" + purpose + ":" + phone; }
    private String coolKey(String purpose, String phone) { return "otp:cool:" + purpose + ":" + phone; }
    private String rateKey(String ip) { return "rate:ip:" + ip; }

    void checkRateLimit(String ip) {
        Long c = redis.opsForValue().increment(rateKey(ip));
        if (c != null && c == 1) redis.expire(rateKey(ip), Duration.ofMinutes(1));
        if (c != null && c > 10) throw new IllegalArgumentException("요청이 너무 많습니다.");
    }

    void ensureResendCooldown(String purpose, String phone, Duration cooldown) {
        String ck = coolKey(purpose, phone);
        Boolean exists = redis.hasKey(ck);
        if (Boolean.TRUE.equals(exists)) throw new IllegalArgumentException("요청이 너무 잦습니다. 잠시 후 다시 시도하세요.");
        redis.opsForValue().set(ck, "1", cooldown);
    }

    void saveOtp(String purpose, String phone, String code, Duration ttl) {
        String key = otpKey(purpose, phone);
        String salt = OtpCrypto.randomSalt();
        String hash = OtpCrypto.sha256(salt + ":" + code);
        redis.opsForHash().putAll(key, Map.of(
                "codeHash", hash,
                "salt", salt,
                "attempts", "0"
        ));
        redis.expire(key, ttl);
    }

    boolean verifyAndConsume(String purpose, String phone, String code, int maxAttempts) {
        String key = otpKey(purpose, phone);

        Map<Object, Object> m = redis.opsForHash().entries(key);
        if (m == null || m.isEmpty() || m.get("salt") == null || m.get("codeHash") == null) {
            throw new IllegalArgumentException("인증 요청이 없습니다.");
        }

        Long tries = redis.opsForHash().increment(key, "attempts", 1L);
        if (tries != null && tries > maxAttempts) {
            throw new IllegalArgumentException("시도 횟수를 초과했습니다.");
        }

        String salt = m.get("salt").toString();
        String expected = m.get("codeHash").toString();
        String given = OtpCrypto.sha256(salt + ":" + code);

        boolean ok = expected.equals(given);
        if (ok) redis.delete(key);
        return ok;
    }
}
