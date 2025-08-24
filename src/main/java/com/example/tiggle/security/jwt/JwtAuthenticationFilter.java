package com.example.tiggle.security.jwt;

import com.example.tiggle.exception.auth.AuthException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null) {
                // 1. 토큰 유효성 검사 및 클레임 추출
                Claims claims = jwtTokenProvider.getClaimsFromToken(jwt);

                // 2. 클레임에서 userId와 암호화된 userKey 추출
                Long userId = claims.get("userId", Long.class);
                String encryptedUserKey = claims.get("userKey", String.class);

                // 3. UserDetailsService를 통해 사용자 정보 로드
                CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(String.valueOf(userId));

                // 4. CustomUserDetails에 encryptedUserKey 설정
                userDetails.setEncryptedUserKey(encryptedUserKey);

                // 5. Spring Security 인증 컨텍스트 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AuthException ex) {
            // JWT 관련 예외 발생 시, SecurityContextHolder를 비우고 응답 상태 설정
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}