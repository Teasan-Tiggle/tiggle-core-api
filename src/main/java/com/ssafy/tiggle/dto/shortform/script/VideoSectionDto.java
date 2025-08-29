package com.ssafy.tiggle.dto.shortform.script;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "영상 섹션 정보 DTO")
public class VideoSectionDto {
    
    @Schema(description = "섹션 번호", example = "1")
    private int sectionNumber;
    
    @Schema(description = "섹션 스크립트", example = "지브리 스타일의 부드러운 애니메이션으로, 따뜻한 자연광이 들어오는 아늑한 카페에서...")
    private String script;
}