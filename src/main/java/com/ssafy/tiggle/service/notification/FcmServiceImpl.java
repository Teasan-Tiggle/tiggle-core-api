package com.ssafy.tiggle.service.notification;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.entity.Users;
import com.ssafy.tiggle.repository.user.StudentRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmServiceImpl implements FcmService {

    private final StudentRepository studentRepository;

    @Override
    public boolean sendNotification(String fcmToken, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지가 성공적으로 전송되었습니다: {}", response);
            return true;
        } catch (Exception e) {
            log.error("FCM 메시지 전송에 실패했습니다.: {}", fcmToken, e);
            return false;
        }
    }

    @Override
    public boolean sendNotificationWithData(String fcmToken, String title, String body, Map<String, String> data) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 메시지와 데이터가 성공적으로 전송되었습니다. title: {}, body: {}", title, body);
            return true;
        } catch (Exception e) {
            log.error("FCM 메시지와 데이터 전송에 실패했습니다: {}", fcmToken, e);
            return false;
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> registerFcmToken(Long userId, String fcmToken) {
        try {
            Users student = studentRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id: " + userId));
            
            student.setFcmToken(fcmToken);
            studentRepository.save(student);
            
            log.info("FCM 토큰 등록에 성공했습니다 id: {}", userId);
            return ApiResponse.success();
        } catch (Exception e) {
            log.error("FCM 토큰 등록에 실패했습니다 id: {}", userId, e);
            return ApiResponse.failure("FCM 토큰 등록에 실패했습니다. 토큰을 다시 확인해주세요.");
        }
    }

    @Override
    @Transactional
    public void removeFcmToken(Long userId) {
        try {
            Users student = studentRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다 id: " + userId));
            
            student.setFcmToken(null);
            studentRepository.save(student);
            
            log.info("FCM 토큰 삭제에 성공했습니다 id: {}", userId);
        } catch (Exception e) {
            log.error("FCM 토큰 삭제에 실패했습니다 id: {}", userId, e);
        }
    }

    @Override
    @Async("fcmAsyncExecutor")
    public CompletableFuture<Void> sendOneWonVerificationNotification(Long userId, String accountNo, String authCode) {
        try {
            Thread.sleep(2000);

            Users student = studentRepository.findById(userId).orElse(null);
            if (student == null || student.getFcmToken() == null || student.getFcmToken().isEmpty()) {
                log.warn("FCM 토큰이 없는 사용자입니다. userId: {}", userId);
                return CompletableFuture.completedFuture(null);
            }
            
            Map<String, String> data = Map.of(
                    "type", "one_won_verification",
                    "accountNo", accountNo,
                    "authCode", authCode != null ? authCode : "인증번호 없음"
            );
            
            String title = "1원이 입금되었습니다.";
            String body = authCode != null 
                    ? String.format("%s", authCode)
                    : "계좌 인증을 위한 1원 송금이 완료되었습니다. 입금 내역을 확인해주세요.";
            
            boolean success = sendNotificationWithData(
                    student.getFcmToken(), 
                    title,
                    body,
                    data
            );
            
            if (success) {
                log.info("FCM 알림이 성공적으로 전송되었습니다. userId: {}, accountNo: {}, authCode: {}", userId, accountNo, authCode);
            } else {
                log.error("FCM 알림 전송에 실패했습니다. userId: {}", userId);
            }
            
        } catch (Exception e) {
            log.error("FCM 알림 전송 중 오류가 발생했습니다. userId: {}", userId, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}