package com.ssafy.tiggle.util;

import com.ssafy.tiggle.security.jwt.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUtil {

    public static Long getCurrentUserId() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userDetails instanceof CustomUserDetails) {
            return ((CustomUserDetails) userDetails).getUserId();
        }
        throw new IllegalStateException("인증된 사용자 정보를 찾을 수 없습니다.");
    }

    public static String getCurrentEncryptedUserKey() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userDetails instanceof CustomUserDetails) {
            return ((CustomUserDetails) userDetails).getEncryptedUserKey();
        }
        throw new IllegalStateException("인증된 사용자의 암호화된 키를 찾을 수 없습니다.");
    }
}