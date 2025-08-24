package com.example.tiggle.service.notification;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.entity.Student;
import com.example.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmTestService {

    private final StudentRepository studentRepository;
    private final FcmService fcmService;

    public ApiResponse<Void> sendNotificationWithData(Integer userId){
        Student student = studentRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id: " + userId));

        String title = "FCM 알림 테스트 Title입니다.";
        String body = "FCM 알림 테스트 Body입니다. 안녕하세요? 반갑습니다?";
        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);
        data.put("data", "더미 데이터입니다. Notification과 Data 모두 잘 수신 되나요?");

        boolean result = fcmService.sendNotificationWithData(student.getFcmToken(), title, body, data);

        if(result)
            return ApiResponse.success();
        else
            return ApiResponse.failure("FCM 메시지와 데이터 전송에 실패했습니다");
    }
}
