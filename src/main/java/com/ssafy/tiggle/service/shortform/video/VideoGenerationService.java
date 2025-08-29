package com.ssafy.tiggle.service.shortform.video;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoGenerationDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoStatusDto;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

public interface VideoGenerationService {
    Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideo(String textPrompt);
    Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideoWithImage(String textPrompt, MultipartFile imageFile);
    Mono<ApiResponse<GeminiVideoStatusDto>> getVideoStatus(String operationName);
    Mono<byte[]> downloadVideo(String operationName);
}