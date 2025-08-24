package com.example.tiggle.service.auth;

import com.example.tiggle.exception.auth.AuthException;
import com.example.tiggle.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    public String reissueAccessToken(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        Claims claims = jwtTokenProvider.getClaimsFromToken(refreshToken);
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new AuthException("INVALID_TOKEN", "토큰의 사용자 정보가 유효하지 않습니다.");
        }

        // 2. Redis에 저장된 토큰과 대조
        Map<Object, Object> storedData = refreshTokenStore.get(userId);
        if (storedData == null || storedData.isEmpty()) {
            throw new AuthException("REFRESH_TOKEN_NOT_FOUND", "로그인 정보가 없습니다. 다시 로그인하세요.");
        }

        String storedRefreshToken = (String) storedData.get("refreshToken");
        if (!refreshToken.equals(storedRefreshToken)) {
            // 중요: Redis의 토큰과 일치하지 않으면, 탈취된 토큰일 수 있으므로 즉시 삭제
            refreshTokenStore.delete(userId);
            throw new AuthException("REFRESH_TOKEN_MISMATCH", "인증 정보가 일치하지 않습니다.");
        }

        // 3. 새로운 Access Token 생성
        String encryptedUserKey = (String) storedData.get("encryptedUserKey");
        if (encryptedUserKey == null) {
            throw new AuthException("USER_KEY_NOT_FOUND", "사용자 키를 찾을 수 없습니다.");
        }

        return jwtTokenProvider.generateAccessToken(userId, encryptedUserKey);
    }
}