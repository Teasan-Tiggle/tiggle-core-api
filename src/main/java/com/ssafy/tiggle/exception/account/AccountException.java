package com.ssafy.tiggle.exception.account;

import lombok.Getter;

@Getter
public class AccountException extends RuntimeException {

    private final String errorCode;

    public AccountException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AccountException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static AccountException invalidAccountNo() {
        return new AccountException("INVALID_ACCOUNT_NO", "올바르지 않은 계좌번호입니다.");
    }

    public static AccountException accountNotFound() {
        return new AccountException("ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다.");
    }

    public static AccountException invalidAuthCode() {
        return new AccountException("INVALID_AUTH_CODE", "인증 코드가 올바르지 않습니다.");
    }

    public static AccountException verificationFailed(String message) {
        return new AccountException("VERIFICATION_FAILED", message != null ? message : "계좌 인증에 실패했습니다.");
    }

    public static AccountException invalidVerificationToken() {
        return new AccountException("INVALID_VERIFICATION_TOKEN", "유효하지 않은 검증 토큰입니다.");
    }

    public static AccountException bankApiError(String message) {
        return new AccountException("BANK_API_ERROR", message != null ? message : "은행 API 오류가 발생했습니다.");
    }

    public static AccountException userNotFound() {
        return new AccountException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
    }

    public static AccountException primaryAccountNotFound() {
        return new AccountException("PRIMARY_ACCOUNT_NOT_FOUND", "등록된 주 계좌가 없습니다.");
    }
}