package com.example.tiggle.exception;

import com.example.tiggle.exception.auth.MailSendException;
import com.example.tiggle.exception.auth.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 메일 전송 관련 예외 처리
     */
    @ExceptionHandler(MailSendException.class)
    public ResponseEntity<ErrorResponse> handleMailSendException(MailSendException e) {
        ErrorResponse response = new ErrorResponse(false, e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 유저 인증 관련 예외 처리
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleUserAuthException(AuthException e) {

        logger.error("사용자 인증/인가 오류: {} (코드: {})", e.getMessage(), e.getErrorCode(), e);

        HttpStatus status = switch (e.getErrorCode()) {
            case "USER_NOT_FOUND", "PASSWORD_MISMATCH" -> HttpStatus.UNAUTHORIZED;
            case "DUPLICATE_EMAIL", "DUPLICATE_STUDENT_ID" -> HttpStatus.CONFLICT;
            case "UNIVERSITY_NOT_FOUND", "DEPARTMENT_NOT_FOUND" -> HttpStatus.BAD_REQUEST;
            case "EXTERNAL_API_FAILURE", "EXTERNAL_API_USER_CREATION_FAILURE", "EXTERNAL_API_USER_NOT_FOUND" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ErrorResponse response = new ErrorResponse(false, e.getMessage());

        return new ResponseEntity<>(response, status);
    }

    /**
     * 잘못된 요청
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse response = new ErrorResponse(false, errorMessage);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 예상치 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception e) {
        logger.error("예상치 못한 서버 오류: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse(false, "서버 처리 중 오류가 발생했습니다, 잠시 후 다시 시도해주세요.");

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 서비스에서 throw한 ResponseStatusException을 그대로 전달
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String msg = (e.getReason() != null) ? e.getReason() : "요청을 처리할 수 없습니다.";
        return ResponseEntity.status(status)
                .body(new ErrorResponse(false, msg));
    }

    // 잘못된 파라미터 등 클라이언트 오류를 400으로
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(false, e.getMessage()));
    }

    // 외부 금융 API(WebClient) 오류를 게이트웨이 오류로 래핑
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClient(WebClientResponseException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(false, "외부 금융 API 오류(" + e.getStatusCode().value() + ")"));
    }
}
