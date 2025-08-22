package com.example.tiggle.controller.user;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.user.JoinRequestDto;
import com.example.tiggle.dto.user.LoginRequestDto;
import com.example.tiggle.security.jwt.JwtTokenProvider;
import com.example.tiggle.service.user.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "유저 API", description = "유저 관련 API")
public class UserController {

    private final StudentService studentService;
    private final JwtTokenProvider jwtTokenProvider;

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
    @Operation(summary = "로그인", description = "사용자 인증을 수행하고 헤더에 JWT를 반환합니다.")
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
        Integer userId = (Integer) loginResult.get("userId");
        String encryptedUserKey = (String) loginResult.get("userKey");

        String token = jwtTokenProvider.generateToken(userId, encryptedUserKey);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        return ResponseEntity.ok().headers(headers).body(new ResponseDto<>(true));
    }
}