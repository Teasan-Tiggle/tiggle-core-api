package com.example.tiggle.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class JoinRequestDto {

    @Email(message = "유효한 이메일 주소를 입력해주세요.")
    @NotBlank(message = "이메일은 필수입니다.")
    @Schema(description = "이메일", example = "example@shinhan.com")
    private String email;

    @NotBlank(message = "이름은 필수입니다.")
    @Schema(description = "이름", example = "홍길동")
    private String name;


    @NotNull(message = "대학은 필수입니다.")
    @Schema(description = "대학교 ID", example = "1")
    private Integer universityId;


    @NotNull(message = "학과는 필수입니다.")
    @Schema(description = "학과 ID", example = "1")
    private Integer departmentId;


    @NotBlank(message = "학번은 필수입니다.")
    @Schema(description = "학번", example = "2020123456")
    private String studentId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "비밀번호", example = "password")
    private String password;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;
}
