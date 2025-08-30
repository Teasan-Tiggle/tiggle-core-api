package com.ssafy.tiggle.controller.test;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.news.CategoryNewsResponseDto;
import com.ssafy.tiggle.dto.shortform.script.VideoSectionDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoGenerationDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoStatusDto;
import com.ssafy.tiggle.service.shortform.news.NewsCrawlerService;
import com.ssafy.tiggle.service.shortform.script.ScriptGenerationService;
import com.ssafy.tiggle.service.shortform.video.VideoGenerationService;
import com.ssafy.tiggle.service.videostorage.VideoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Short-form Pipeline Test", description = "숏폼 비디오 생성 파이프라인 각 단계별 테스트 API")
public class TestController {

    private final ScriptGenerationService scriptGenerationService;
    private final NewsCrawlerService newsCrawlerService;
    private final VideoGenerationService videoGenerationService;

    @Operation(summary = "전체 카테고리 헤드라인 뉴스 조회", description = "네이버 뉴스의 모든 카테고리별 헤드라인 뉴스를 조회합니다.")
    @GetMapping("/news")
    public ResponseEntity<ApiResponse<List<CategoryNewsResponseDto>>> getHeadlineNews() {
        log.info("헤드라인 뉴스 조회 테스트 요청");
        ApiResponse<List<CategoryNewsResponseDto>> response = newsCrawlerService.crawlAllCategoryHeadlines().block();
        
        if (response != null && response.isResult()) {
            log.info("헤드라인 뉴스 조회 성공 - {} 카테고리", response.getData().size());
            return ResponseEntity.ok(response);
        } else {
            log.error("헤드라인 뉴스 조회 실패: {}", response != null ? response.getMessage() : "null response");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/script")
    @Operation(summary = "숏폼 비디오 스크립트 생성 테스트", description = "제목과 본문을 바탕으로 숏폼 비디오 스크립트를 생성합니다.")
    public ResponseEntity<ApiResponse<List<VideoSectionDto>>> testScriptGeneration(
            @Parameter(description = "뉴스 제목", example = "AI 기술의 발전과 미래 전망")
            @RequestParam String title,
            @Parameter(description = "뉴스 본문", example = "인공지능 기술이 빠르게 발전하면서...")
            @RequestBody String body) {

        log.info("스크립트 생성 테스트 요청 - 제목: {}", title);

        ApiResponse<List<VideoSectionDto>> response = scriptGenerationService.generateShortFormVideoScript(title, body).block();
        
        if (response != null && response.isResult()) {
            log.info("스크립트 생성 성공 - {} 섹션", response.getData().size());
            return ResponseEntity.ok(response);
        } else {
            log.error("스크립트 생성 실패: {}", response != null ? response.getMessage() : "null response");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/video/generate")
    @Operation(summary = "Gemini 비디오 생성", description = "텍스트 프롬프트를 사용하여 Gemini API로 비디오를 생성합니다")
    public ResponseEntity<ApiResponse<GeminiVideoGenerationDto>> generateVideo(
            @Parameter(description = "비디오 생성을 위한 텍스트 프롬프트", example = "A serene sunset over a calm ocean with gentle waves")
            @RequestParam String prompt) {
        
        log.info("Gemini 비디오 생성 요청 - 프롬프트: {}", prompt);
        
        try {
            ApiResponse<GeminiVideoGenerationDto> response = videoGenerationService.generateVideo(prompt).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Gemini 비디오 생성 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.failure("Gemini 비디오 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/video/generate-with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Gemini 이미지 기반 비디오 생성", description = "텍스트 프롬프트와 이미지를 사용하여 Gemini API로 비디오를 생성합니다")
    public ResponseEntity<ApiResponse<GeminiVideoGenerationDto>> generateVideoWithImage(
            @Parameter(description = "비디오 생성을 위한 텍스트 프롬프트", example = "Animate this image with gentle movement")
            @RequestParam String textPrompt,
            @Parameter(description = "애니메이션을 적용할 이미지 파일")
            @RequestParam MultipartFile image) {
        
        log.info("Gemini 이미지 기반 비디오 생성 요청 - 프롬프트: {}, 이미지: {}", textPrompt, image.getOriginalFilename());
        
        try {
            ApiResponse<GeminiVideoGenerationDto> response = videoGenerationService.generateVideoWithImage(textPrompt, image).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Gemini 이미지 기반 비디오 생성 실패", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.failure("Gemini 이미지 기반 비디오 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/video/status")
    @Operation(summary = "Gemini 비디오 생성 상태 확인", description = "Operation Name을 사용하여 Gemini 비디오 생성 상태를 확인합니다")
    public ResponseEntity<ApiResponse<GeminiVideoStatusDto>> getVideoStatus(
            @Parameter(description = "Operation Name", example = "models/veo-3.0-fast-generate-preview/operations/7xm0svrbukrn")
            @RequestParam String operationName) {
        
        log.info("Gemini 비디오 상태 확인 요청 - Operation Name: {}", operationName);
        
        try {
            ApiResponse<GeminiVideoStatusDto> response = videoGenerationService.getVideoStatus(operationName).block();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Gemini 비디오 상태 확인 실패 - Operation Name: {}", operationName, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.failure("Gemini 비디오 상태 확인 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/video/download")
    @Operation(summary = "Gemini 비디오 다운로드", description = "Operation Name을 사용하여 생성된 비디오를 다운로드합니다")
    public ResponseEntity<byte[]> downloadVideo(
            @Parameter(description = "Operation Name", example = "models/veo-3.0-fast-generate-preview/operations/7xm0svrbukrn")
            @RequestParam String operationName) {
        
        log.info("Gemini 비디오 다운로드 요청 - Operation Name: {}", operationName);
        
        try {
            byte[] videoBytes = videoGenerationService.downloadVideo(operationName).block();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4"));
            headers.setContentDispositionFormData("attachment", "gemini_generated_video.mp4");
            headers.setContentLength(videoBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(videoBytes);
        } catch (Exception e) {
            log.error("Gemini 비디오 다운로드 실패 - Operation Name: {}", operationName, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/video/extract-last-frame")
    @Operation(summary = "비디오 마지막 프레임 추출 테스트", description = "Operation Name으로 비디오를 다운로드하고 마지막 프레임을 이미지로 추출합니다")
    public ResponseEntity<byte[]> extractVideoLastFrame(
            @Parameter(description = "Operation Name", example = "models/veo-3.0-fast-generate-preview/operations/7xm0svrbukrn")
            @RequestParam String operationName) {
        
        log.info("비디오 마지막 프레임 추출 요청 - Operation Name: {}", operationName);
        
        try {
            byte[] frameBytes = videoGenerationService.extractVideoLastFrame(operationName).block();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("image/jpeg"));
            headers.setContentDispositionFormData("attachment", "last_frame.jpg");
            headers.setContentLength(frameBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(frameBytes);
        } catch (Exception e) {
            log.error("비디오 마지막 프레임 추출 실패 - Operation Name: {}", operationName, e);
            return ResponseEntity.status(500)
                    .body(("프레임 추출 실패: " + e.getMessage()).getBytes());
        }
    }
    
    @PostMapping("/video/full-generation")
    @Operation(summary = "풀 비디오 생성 파이프라인 테스트", description = "스크립트 섹션 리스트로부터 연속된 풀 영상을 생성하고 다운로드합니다")
    public ResponseEntity<byte[]> testFullVideoGeneration(
            @Parameter(description = "비디오 섹션 리스트")
            @RequestBody List<VideoSectionDto> sections) {
        
        log.info("풀 비디오 생성 파이프라인 테스트 요청 - {} 섹션", sections.size());
        
        try {
            ApiResponse<byte[]> response = videoGenerationService.generateFullVideoFromSections(sections).block();
            
            if (response != null && response.isResult()) {
                byte[] videoBytes = response.getData();
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("video/mp4"));
                headers.setContentDispositionFormData("attachment", "full_generated_video.mp4");
                headers.setContentLength(videoBytes.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(videoBytes);
            } else {
                log.error("풀 비디오 생성 실패: {}", response != null ? response.getMessage() : "null response");
                return ResponseEntity.status(500)
                        .body(("풀 비디오 생성 실패: " + (response != null ? response.getMessage() : "null response")).getBytes());
            }
            
        } catch (Exception e) {
            log.error("풀 비디오 생성 파이프라인 테스트 실패", e);
            return ResponseEntity.status(500)
                    .body(("풀 비디오 생성 실패: " + e.getMessage()).getBytes());
        }
    }
    
    @PostMapping(value = "/video/concatenate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "두 비디오 파일 합치기 테스트", description = "두 개의 MP4 파일을 업로드하여 하나의 연결된 비디오로 합칩니다")
    public ResponseEntity<byte[]> concatenateTwoVideos(
            @Parameter(description = "첫 번째 비디오 파일")
            @RequestParam MultipartFile video1,
            @Parameter(description = "두 번째 비디오 파일")
            @RequestParam MultipartFile video2) {
        
        log.info("두 비디오 합치기 테스트 요청 - 파일1: {} ({} bytes), 파일2: {} ({} bytes)", 
                video1.getOriginalFilename(), video1.getSize(),
                video2.getOriginalFilename(), video2.getSize());
        
        try {
            // 업로드된 파일들을 byte[] 리스트로 변환
            List<byte[]> videoList = Arrays.asList(
                video1.getBytes(),
                video2.getBytes()
            );
            
            // VideoGenerationService의 concatenateVideos 메서드 직접 호출
            // private 메서드이므로 public 메서드를 통해 테스트
            byte[] concatenatedVideo = testVideoConcatenation(videoList);
            
            if (concatenatedVideo != null && concatenatedVideo.length > 0) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("video/mp4"));
                headers.setContentDispositionFormData("attachment", "concatenated_video.mp4");
                headers.setContentLength(concatenatedVideo.length);
                
                log.info("두 비디오 합치기 성공 - 최종 크기: {} bytes", concatenatedVideo.length);
                
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(concatenatedVideo);
            } else {
                log.error("비디오 합치기 결과가 비어있습니다");
                return ResponseEntity.status(500)
                        .body("비디오 합치기 실패: 결과가 비어있습니다".getBytes());
            }
            
        } catch (Exception e) {
            log.error("두 비디오 합치기 테스트 실패", e);
            return ResponseEntity.status(500)
                    .body(("비디오 합치기 실패: " + e.getMessage()).getBytes());
        }
    }
    
    private byte[] testVideoConcatenation(List<byte[]> videoList) {
        return videoGenerationService.testVideoConcatenation(videoList);
    }

    private final VideoStorageService videoStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadVideo(@RequestParam MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid file name.");
        }
        videoStorageService.saveVideo(file.getBytes(), filename);
        return ResponseEntity.ok("Video uploaded successfully: " + filename);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadVideo2(@RequestParam("path") String path) throws IOException {
        byte[] videoBytes = videoStorageService.loadVideo(path);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        headers.setContentDispositionFormData("attachment", "video.mp4");
        headers.setContentLength(videoBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(videoBytes);
    }
}