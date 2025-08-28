package com.example.tiggle.dto.video;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비디오 생성 응답")
public class VideoGenerationResponse {

    @Schema(description = "작업 ID", example = "operations/generate_12345")
    private String operationId;

    @Schema(description = "작업 상태", example = "PROCESSING", allowableValues = {"PROCESSING", "COMPLETED", "FAILED", "ERROR"})
    private String status;

    @Schema(description = "비디오 URL (완료시)", example = "https://storage.googleapis.com/video/generated_video.mp4")
    private String videoUrl;

    @Schema(description = "오류 메시지 (실패시)", example = "Generation failed due to invalid prompt")
    private String errorMessage;

    @Schema(description = "예상 완료 시간 (초)", example = "120")
    private Integer estimatedCompletionTime;
}