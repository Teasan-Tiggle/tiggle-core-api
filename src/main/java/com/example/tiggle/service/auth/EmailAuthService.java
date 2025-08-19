package com.example.tiggle.service.auth;

public interface EmailAuthService {
    /**
     * 지정된 이메일로 인증 코드를 발송합니다.
     * @param email 인증 코드를 받을 이메일 주소
     */
    void sendVerificationCode(String email);
}
