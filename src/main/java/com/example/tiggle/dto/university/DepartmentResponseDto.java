package com.example.tiggle.dto.university;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DepartmentResponseDto {

    @Schema(description = "학과 ID", example = "1")
    private Long id;

    @Schema(description = "학과명", example = "컴퓨터공학과")
    private String name;
}
