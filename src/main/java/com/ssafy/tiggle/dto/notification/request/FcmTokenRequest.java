package com.ssafy.tiggle.dto.notification.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FCM 토큰 등록 요청 DTO")
public class FcmTokenRequest {
    
    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @Schema(description = "FCM 토큰", example = "dY4iGn5CQ-2lHjkfGzDQBE:APA91bF...")
    private String fcmToken;
}