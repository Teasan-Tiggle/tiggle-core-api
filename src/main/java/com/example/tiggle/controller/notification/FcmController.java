package com.example.tiggle.controller.notification;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.notification.request.FcmTokenRequest;
import com.example.tiggle.service.notification.FcmService;
import com.example.tiggle.service.notification.FcmTestService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
@Tag(name = "FCM API", description = "FCM 푸시 알림 관련 API")
public class FcmController {

    private final FcmService fcmService;
    private final FcmTestService fcmTestService;

    @PostMapping("/token")
    @Operation(summary = "FCM 토큰 등록", description = "사용자의 FCM 토큰을 등록/업데이트합니다.")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @Valid @RequestBody FcmTokenRequest request) {
        Integer userId = JwtUtil.getCurrentUserId();

        try {
            ApiResponse<Void> response = fcmService.registerFcmToken(userId, request.getFcmToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/send")
    @Operation(summary = "FCM 알림 전송 테스트", description = "현재 로그인한 계정으로 FCM을 통해 푸시 알림을 전송합니다.")
    public ResponseEntity<ApiResponse<Void>> sendNotification() {
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<Void> response = fcmTestService.sendNotificationWithData(userId);

        return ResponseEntity.ok(response);
    }
}