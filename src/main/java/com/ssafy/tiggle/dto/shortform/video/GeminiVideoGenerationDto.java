package com.ssafy.tiggle.dto.shortform.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gemini 비디오 생성 요청 응답 DTO")
public class GeminiVideoGenerationDto {

    @Schema(description = "작업 식별자", example = "models/veo-3.0-fast-generate-preview/operations/7xm0svrbukrn")
    private String operationName;

    public static GeminiVideoGenerationDto of(String operationName) {
        return new GeminiVideoGenerationDto(operationName);
    }
}