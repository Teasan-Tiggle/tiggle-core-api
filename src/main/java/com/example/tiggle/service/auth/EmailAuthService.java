package com.example.tiggle.service.auth;

public interface EmailAuthService {
    /**
     * 지정된 이메일로 인증 코드를 발송합니다.
     * @param email 인증 코드를 받을 이메일 주소
     */
    void sendVerificationCode(String email);

    /**
     * 사용자가 입력한 코드가 유효한지 검증합니다.
     * @param email 검증할 이메일 주소
     * @param code 사용자가 입력한 인증 코드
     * @return 코드의 유효성 여부
     */
    boolean verifyCode(String email, String code);
}
