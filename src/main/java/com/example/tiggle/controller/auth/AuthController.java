package com.example.tiggle.controller.auth;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.auth.JoinRequestDto;
import com.example.tiggle.dto.auth.LoginRequestDto;
import com.example.tiggle.security.jwt.CustomUserDetails;
import com.example.tiggle.security.jwt.JwtTokenProvider;
import com.example.tiggle.service.auth.RefreshTokenService;
import com.example.tiggle.service.auth.RefreshTokenStore;
import com.example.tiggle.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "유저 인증 API", description = "회원가입 및 로그인/로그아웃")
public class AuthController {

    private final AuthService studentService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-expiration-time}")
    private long refreshTokenExpirationTime;

    /**
     * 회원가입
     *
     * @param requestDto 회원가입 정보
     * @return 회원가입 결과
     */
    @PostMapping("/join")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 유저"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<Void>> join(
            @Parameter(description = "회원가입 정보 (JSON 형식)", required = true)
            @Valid @RequestBody JoinRequestDto requestDto
    ) {
        studentService.joinUser(requestDto);
        return ResponseEntity.ok(new ResponseDto<>(true, "회원가입 성공"));
    }

    /**
     * 로그인
     *
     * @param requestDto 로그인 정보
     * @return 로그인 결과 (헤더에 JWT 포함)
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증을 수행하고 JWT를 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<Void>> login(
            @Parameter(description = "로그인 정보 (JSON 형식)", required = true)
            @Valid @RequestBody LoginRequestDto requestDto
    ) {
        Map<String, Object> loginResult = studentService.loginUser(requestDto.getEmail(), requestDto.getPassword());
        Long userId = (Long) loginResult.get("userId");
        String encryptedUserKey = (String) loginResult.get("userKey");

        String accessToken = jwtTokenProvider.generateAccessToken(userId, encryptedUserKey);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // Redis에 리프레시 토큰 저장
        refreshTokenStore.save(userId, refreshToken, encryptedUserKey, Duration.ofMillis(refreshTokenExpirationTime));

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMillis(refreshTokenExpirationTime))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok().headers(headers).body(new ResponseDto<>(true));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<Void>> logout() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            refreshTokenStore.delete(userDetails.getUserId());
        }

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok().headers(headers).body(new ResponseDto<>(true));
    }

    @PostMapping("/reissue")
    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "액세스 토큰 재발급 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<Void>> refresh(HttpServletRequest request) {
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).body(new ResponseDto<>(false, "리프레시 토큰이 없습니다."));
        }

        String newAccessToken = refreshTokenService.reissueAccessToken(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + newAccessToken);

        return ResponseEntity.ok().headers(headers).body(new ResponseDto<>(true));
    }
}
