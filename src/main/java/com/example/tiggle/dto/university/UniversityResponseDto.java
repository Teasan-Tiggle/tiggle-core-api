package com.example.tiggle.dto.university;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class UniversityResponseDto {

    @Schema(description = "대학 ID", example = "1")
    private Integer id;

    @Schema(description = "대학명", example = "신한대학교")
    private String name;
}
