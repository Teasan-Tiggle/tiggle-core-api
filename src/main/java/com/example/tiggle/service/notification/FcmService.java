package com.example.tiggle.service.notification;

import com.example.tiggle.dto.common.ApiResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface FcmService {
    
    boolean sendNotification(String fcmToken, String title, String body);
    
    boolean sendNotificationWithData(String fcmToken, String title, String body, Map<String, String> data);
    
    ApiResponse<Void> registerFcmToken(Long userId, String fcmToken);
    
    void removeFcmToken(Long userId);
    
    CompletableFuture<Void> sendOneWonVerificationNotification(Long userId, String accountNo, String authCode);
}