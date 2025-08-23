package com.example.tiggle.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redis;

    private String refreshTokenKey(int userId) {
        return "refreshToken:" + userId;
    }

    public void save(int userId, String refreshToken, String encryptedUserKey, Duration ttl) {
        String key = refreshTokenKey(userId);
        redis.opsForHash().putAll(key, Map.of(
                "refreshToken", refreshToken,
                "encryptedUserKey", encryptedUserKey
        ));
        redis.expire(key, ttl);
    }

    public Map<Object, Object> get(int userId) {
        String key = refreshTokenKey(userId);
        return redis.opsForHash().entries(key);
    }

    public void delete(int userId) {
        String key = refreshTokenKey(userId);
        redis.delete(key);
    }
}