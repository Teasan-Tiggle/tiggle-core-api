package com.example.tiggle.controller.auth;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.auth.EmailSendRequestDto;
import com.example.tiggle.dto.auth.EmailVerifyRequestDto;
import com.example.tiggle.service.auth.EmailAuthService;
import com.example.tiggle.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/email")
@Tag(name = "이메일 인증 API", description = "이메일로 인증코드를 발급하고 검증합니다.")
public class EmailAuthController {

    private final AuthService studentService;
    private final EmailAuthService emailAuthService;

    /**
     * 입력한 이메일로 인증번호를 전송한다.
     *
     * @return 전송 결과
     */
    @PostMapping("/send")
    @Operation(summary = "이메일 인증코드 전송", description = "입력한 이메일 주소로 인증코드를 전송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 번호 전송 결과 반환"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (이메일 누락)", content = @Content)
    })
    public ResponseEntity<?> sendEmail(
            @Parameter(description = "이메일 정보 (JSON 형식)", required = true)
            @Valid @RequestBody EmailSendRequestDto requestDto
    ) {

        if (requestDto.getEmail() == null)
            return ResponseEntity.badRequest().build();

        emailAuthService.sendVerificationCode(requestDto.getEmail());

        return ResponseEntity.ok(new ResponseDto<Void>(true));
    }

    /**
     * 입력한 이메일과 인증코드로 검증한다.
     *
     * @return 검증 결과
     */
    @PostMapping("/verify")
    @Operation(summary = "이메일 인증코드 검증", description = "입력한 이메일과 인증코드를 검증합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 인증 결과 반환"),
            @ApiResponse(responseCode = "400", description = "요청 형식 오류 (이메일 또는 코드 누락)", content = @Content)
    })
    public ResponseEntity<ResponseDto<Void>> verifyCode(
            @Parameter(description = "이메일 인증코드 정보 (JSON 형식)", required = true)
            @Valid @RequestBody EmailVerifyRequestDto requestDto
    ) {
        boolean isVerified = emailAuthService.verifyCode(requestDto.getEmail(), requestDto.getCode());
        if (isVerified) {
            return ResponseEntity.ok(new ResponseDto<Void>(true));
        } else {
            return ResponseEntity.badRequest().body(new ResponseDto<Void>(false, "인증 코드가 유효하지 않습니다."));
        }
    }
}