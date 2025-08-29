package com.ssafy.tiggle.service.auth;

public interface RefreshTokenService {
    String reissueAccessToken(String refreshToken);
}
