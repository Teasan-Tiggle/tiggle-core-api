package com.example.tiggle.controller.user;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.user.JoinRequestDto;
import com.example.tiggle.service.user.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = "유저 API", description = "유저 관련 API")
public class UserController {

    private final StudentService studentService;

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
            @ApiResponse(responseCode = "409", description = "이메일 또는 학번 중복")
    })
    public ResponseEntity<ResponseDto<Void>> join(
            @Parameter(description = "회원가입 정보 (JSON 형식)", required = true)
            @Valid @RequestBody JoinRequestDto requestDto
    ) {
        boolean isJoined = studentService.joinUser(requestDto);

        if (isJoined) {
            return ResponseEntity.ok(new ResponseDto<Void>(true, "회원가입 성공"));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ResponseDto<Void>(false, "이미 가입된 이메일 또는 학번입니다."));
        }
    }
}
