package com.example.tiggle.service.notification;

import com.example.tiggle.dto.common.ApiResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface FcmService {
    
    boolean sendNotification(String fcmToken, String title, String body);
    
    boolean sendNotificationWithData(String fcmToken, String title, String body, Map<String, String> data);
    
    ApiResponse<Void> registerFcmToken(Integer studentId, String fcmToken);
    
    void removeFcmToken(Integer studentId);
    
    CompletableFuture<Void> sendOneWonVerificationNotification(Integer userId, String accountNo, String authCode);
}