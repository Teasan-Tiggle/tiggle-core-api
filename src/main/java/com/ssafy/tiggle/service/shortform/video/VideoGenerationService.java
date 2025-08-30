package com.ssafy.tiggle.service.shortform.video;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.script.VideoSectionDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoGenerationDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoStatusDto;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import java.util.List;

public interface VideoGenerationService {
    Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideo(String textPrompt);
    Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideoWithImage(String textPrompt, MultipartFile imageFile);
    Mono<ApiResponse<GeminiVideoStatusDto>> getVideoStatus(String operationName);
    Mono<byte[]> downloadVideo(String operationName);
    
    /**
     * 스크립트 섹션 리스트로부터 연속된 풀 영상을 생성합니다.
     * 각 섹션별로 8초 영상을 생성하고, 이전 영상의 마지막 프레임을 다음 영상의 시작 이미지로 사용합니다.
     */
    Mono<ApiResponse<byte[]>> generateFullVideoFromSections(List<VideoSectionDto> sections);
    
    /**
     * Operation Name으로 비디오를 다운로드하고 마지막 프레임을 이미지로 추출합니다.
     * 프레임 추출 기능 테스트용 API입니다.
     */
    Mono<byte[]> extractVideoLastFrame(String operationName);
    
    /**
     * 비디오 합치기 기능 테스트용 메서드입니다.
     * 여러 비디오 byte[] 배열을 받아서 하나로 연결합니다.
     */
    byte[] testVideoConcatenation(List<byte[]> videoList);
}