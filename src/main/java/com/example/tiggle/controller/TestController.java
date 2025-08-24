package com.example.tiggle.controller;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.security.jwt.CustomUserDetails;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class TestController {

    private final EncryptionService encryptionService;

    @GetMapping("/protected")
    public ResponseEntity<ResponseDto<String>> getProtectedData(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Long userId = userDetails.getUserId();
        String encryptedUserKey = userDetails.getEncryptedUserKey();

        // 암호화된 userKey 복호화
        String decryptedUserKey = encryptionService.decrypt(encryptedUserKey);

        return ResponseEntity.ok(new ResponseDto<>(true, "보호된 데이터 접근 성공! userId: " + userId + ", 복호화된 userKey: " + decryptedUserKey));
    }
}