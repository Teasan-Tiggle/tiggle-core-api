package com.ssafy.tiggle.controller;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.VideoResponseDto;
import com.ssafy.tiggle.dto.shortform.UserNewsRequestDto;
import com.ssafy.tiggle.service.shortform.ShortFormPipelineService;
import com.ssafy.tiggle.service.shortform.video.VideoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shortform")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Short-form Video", description = "숏폼 비디오 생성 및 관리 API")
public class ShortFormController {
    
    private final ShortFormPipelineService pipelineService;
    private final VideoService videoService;
    
    @PostMapping("/generate")
    @Operation(summary = "숏폼 비디오 생성", description = "뉴스 크롤링부터 비디오 생성까지 전체 파이프라인을 실행합니다")
    public ResponseEntity<ApiResponse<VideoResponseDto>> generateVideo() {
        
        log.info("숏폼 비디오 생성 요청");
        
        try {
            ApiResponse<VideoResponseDto> apiResponse = pipelineService.generateShortFormVideo().block();
            
            if (apiResponse != null && apiResponse.isResult()) {
                log.info("숏폼 비디오 생성 완료 - {}", apiResponse.getData().getTitle());
                return ResponseEntity.ok(apiResponse);
            } else {
                log.error("숏폼 비디오 생성 실패 - {}", apiResponse != null ? apiResponse.getMessage() : "null response");
                return ResponseEntity.status(500).body(apiResponse != null ? apiResponse : 
                    ApiResponse.failure("비디오 생성에 실패했습니다."));
            }
        } catch (Exception e) {
            log.error("숏폼 비디오 생성 API 오류", e);
            ApiResponse<VideoResponseDto> errorResponse = ApiResponse.failure("서버 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/generate/custom")
    @Operation(summary = "사용자 입력 뉴스로 숏폼 비디오 생성", description = "사용자가 입력한 뉴스 제목과 본문으로 숏폼 비디오를 생성합니다")
    public ResponseEntity<ApiResponse<VideoResponseDto>> generateVideoFromUserNews(
            @Valid @RequestBody UserNewsRequestDto request) {
        
        log.info("사용자 입력 뉴스 숏폼 비디오 생성 요청 - title: {}", request.getTitle());
        
        try {
            ApiResponse<VideoResponseDto> apiResponse = pipelineService.generateShortFormVideoFromNews(
                request.getTitle(), 
                request.getBody()
            ).block();
            
            if (apiResponse != null && apiResponse.isResult()) {
                log.info("사용자 입력 뉴스 숏폼 비디오 생성 완료 - {}", apiResponse.getData().getTitle());
                return ResponseEntity.ok(apiResponse);
            } else {
                log.error("사용자 입력 뉴스 숏폼 비디오 생성 실패 - {}", apiResponse != null ? apiResponse.getMessage() : "null response");
                return ResponseEntity.status(500).body(apiResponse != null ? apiResponse : 
                    ApiResponse.failure("비디오 생성에 실패했습니다."));
            }
        } catch (Exception e) {
            log.error("사용자 입력 뉴스 숏폼 비디오 생성 API 오류", e);
            ApiResponse<VideoResponseDto> errorResponse = ApiResponse.failure("서버 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @GetMapping("/download/{videoId}")
    @Operation(summary = "숏폼 비디오 다운로드", description = "생성된 숏폼 비디오를 다운로드합니다")
    public ResponseEntity<byte[]> downloadVideo(
            @Parameter(description = "비디오 ID", example = "1")
            @PathVariable Long videoId) {
        
        try {
            log.info("숏폼 비디오 다운로드 요청 - ID: {}", videoId);
            
            byte[] videoBytes = videoService.downloadVideo(videoId);
            var video = videoService.findById(videoId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4"));
            headers.setContentDispositionFormData("attachment", video.getId() + ".mp4");
            headers.setContentLength(videoBytes.length);
            
            log.info("숏폼 비디오 다운로드 완료 - ID: {}, 크기: {} bytes", videoId, videoBytes.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(videoBytes);
                
        } catch (Exception e) {
            log.error("숏폼 비디오 다운로드 실패 - ID: {}", videoId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{videoId}")
    @Operation(summary = "숏폼 비디오 정보 조회", description = "숏폼 비디오의 메타데이터를 조회합니다")
    public ResponseEntity<ApiResponse<VideoResponseDto>> getVideoInfo(
            @Parameter(description = "비디오 ID", example = "1")
            @PathVariable Long videoId) {
        
        try {
            log.info("숏폼 비디오 정보 조회 요청 - ID: {}", videoId);
            
            var video = videoService.findById(videoId);
            VideoResponseDto responseDto = VideoResponseDto.from(video);
            
            return ResponseEntity.ok(ApiResponse.success(responseDto));
            
        } catch (Exception e) {
            log.error("숏폼 비디오 정보 조회 실패 - ID: {}", videoId, e);
            return ResponseEntity.status(404)
                .body(ApiResponse.failure("비디오를 찾을 수 없습니다: " + videoId));
        }
    }
}