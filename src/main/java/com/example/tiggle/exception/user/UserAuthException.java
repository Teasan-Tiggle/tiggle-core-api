package com.example.tiggle.exception.user;

import lombok.Getter;

@Getter
public class UserAuthException extends RuntimeException {

    private final String errorCode;

    public UserAuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserAuthException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public static UserAuthException userNotFound() {
        return new UserAuthException("USER_NOT_FOUND", "이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    public static UserAuthException passwordMismatch() {
        return new UserAuthException("PASSWORD_MISMATCH", "이메일 또는 비밀번호가 일치하지 않습니다.");
    }

    public static UserAuthException duplicateEmail() {
        return new UserAuthException("DUPLICATE_EMAIL", "이미 가입된 이메일입니다.");
    }

    public static UserAuthException universityNotFound(Integer universityId) {
        return new UserAuthException("UNIVERSITY_NOT_FOUND", "대학교를 찾을 수 없습니다: " + universityId);
    }

    public static UserAuthException departmentNotFound(Integer departmentId) {
        return new UserAuthException("DEPARTMENT_NOT_FOUND", "학과를 찾을 수 없습니다: " + departmentId);
    }

    public static UserAuthException duplicateStudent(Integer universityId, String studentId) {
        return new UserAuthException("DUPLICATE_STUDENT", "이미 가입된 학적 정보입니다: " + universityId + ", " + studentId);
    }

    public static UserAuthException externalApiFailure(String message, Throwable cause) {
        return new UserAuthException("EXTERNAL_API_FAILURE", "금융 API 연동 중 오류가 발생했습니다: " + message, cause);
    }

    public static UserAuthException externalApiUserCreationFailure(String message, Throwable cause) {
        return new UserAuthException("EXTERNAL_API_USER_CREATION_FAILURE", "금융 API 사용자 생성에 실패했습니다: " + message, cause);
    }

    public static UserAuthException externalApiUserNotFound() {
        return new UserAuthException("EXTERNAL_API_USER_NOT_FOUND", "금융 API에서 사용자 정보를 찾을 수 없습니다.");
    }
}