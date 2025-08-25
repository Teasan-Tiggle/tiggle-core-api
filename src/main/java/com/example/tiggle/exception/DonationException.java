package com.example.tiggle.exception;

import lombok.Getter;

@Getter
public class DonationException extends RuntimeException {

    private final String errorCode;

    public DonationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DonationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static DonationException universityAccountNotFound() {
        return new DonationException("UNIVERSITY_ACCOUNT_NOT_FOUND", "학교의 기부 계좌 정보가 없습니다.");
    }

    public static DonationException userAccountNotFound() {
        return new DonationException("USER_ACCOUNT_NOT_FOUND", "사용자의 주계좌 정보가 없습니다.");
    }

    public static DonationException accountBalance(Long balance) {
        return new DonationException("ACCOUNT_BALANCE_LACK", "계좌 잔고가 부족합니다. 잔액: " + balance);
    }
}