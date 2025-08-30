package com.ssafy.tiggle.dto.shortform;

import com.ssafy.tiggle.entity.Video;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비디오 응답 DTO")
public class VideoResponseDto {
    
    @Schema(description = "비디오 ID", example = "1")
    private Long id;
    
    @Schema(description = "비디오 제목", example = "AI 기술의 발전과 미래 전망")
    private String title;
    
    @Schema(description = "ESG 카테고리명", example = "Planet")
    private String esgCategoryName;
    
    @Schema(description = "다운로드 URL", example = "/api/shortform/download/1")
    private String downloadUrl;
    
    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;
    
    public static VideoResponseDto from(Video video) {
        return new VideoResponseDto(
            video.getId(),
            video.getTitle(),
            video.getEsgCategory() != null ? video.getEsgCategory().getName() : null,
            "/api/shortform/download/" + video.getId(),
            video.getCreatedAt()
        );
    }
}