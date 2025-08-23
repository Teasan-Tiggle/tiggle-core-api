package com.example.tiggle.exception.auth;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final String errorCode;

    public AuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static AuthException userNotFound() {
        return new AuthException("USER_NOT_FOUND", "이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    public static AuthException passwordMismatch() {
        return new AuthException("PASSWORD_MISMATCH", "이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    public static AuthException duplicateEmail() {
        return new AuthException("DUPLICATE_EMAIL", "이미 가입된 이메일입니다.");
    }

    public static AuthException universityNotFound(Integer universityId) {
        return new AuthException("UNIVERSITY_NOT_FOUND", "대학교를 찾을 수 없습니다: " + universityId);
    }

    public static AuthException departmentNotFound(Integer departmentId) {
        return new AuthException("DEPARTMENT_NOT_FOUND", "학과를 찾을 수 없습니다: " + departmentId);
    }

    public static AuthException duplicateStudent(Integer universityId, String studentId) {
        return new AuthException("DUPLICATE_STUDENT", "이미 가입된 학적 정보입니다: " + universityId + ", " + studentId);
    }

    public static AuthException externalApiFailure(String message, Throwable cause) {
        return new AuthException("EXTERNAL_API_FAILURE", "금융 API 연동 중 오류가 발생했습니다: " + message, cause);
    }

    public static AuthException externalApiUserCreationFailure(String message, Throwable cause) {
        return new AuthException("EXTERNAL_API_USER_CREATION_FAILURE", "금융 API 사용자 생성에 실패했습니다: " + message, cause);
    }

    public static AuthException externalApiUserNotFound() {
        return new AuthException("EXTERNAL_API_USER_NOT_FOUND", "금융 API에서 사용자 정보를 찾을 수 없습니다.");
    }
}