package com.example.tiggle.service.auth;

public interface RefreshTokenService {
    String reissueAccessToken(String refreshToken);
}
