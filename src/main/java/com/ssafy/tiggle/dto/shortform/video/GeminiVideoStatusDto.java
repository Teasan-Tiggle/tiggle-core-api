package com.ssafy.tiggle.dto.shortform.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gemini 비디오 생성 상태 응답 DTO")
public class GeminiVideoStatusDto {

    @Schema(description = "작업 식별자", example = "models/veo-3.0-fast-generate-preview/operations/7xm0svrbukrn")
    private String operationName;

    @Schema(description = "작업 완료 여부", example = "true")
    private boolean done;

    @Schema(description = "비디오 다운로드 URI (완료 시에만 포함)", example = "https://generativelanguage.googleapis.com/v1beta/files/8ozvmvtfja39:download?alt=media")
    private String videoUri;

    public static GeminiVideoStatusDto processing(String operationName) {
        return new GeminiVideoStatusDto(operationName, false, null);
    }

    public static GeminiVideoStatusDto completed(String operationName, String videoUri) {
        return new GeminiVideoStatusDto(operationName, true, videoUri);
    }
}