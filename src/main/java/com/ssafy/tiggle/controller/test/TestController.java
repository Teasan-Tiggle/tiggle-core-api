package com.ssafy.tiggle.controller.test;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.news.CategoryNewsResponseDto;
import com.ssafy.tiggle.dto.video.VideoGenerationResponse;
import com.ssafy.tiggle.service.image.ImageGenerationService;
import com.ssafy.tiggle.service.news.NewsCrawlerService;
import com.ssafy.tiggle.service.openai.OpenAiService;
import com.ssafy.tiggle.service.tts.TextToSpeechService;
import com.ssafy.tiggle.service.video.VideoGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Short-form Pipeline Test", description = "숏폼 비디오 생성 파이프라인 각 단계별 테스트 API")
public class TestController {

    private final TextToSpeechService textToSpeechService;
    private final ImageGenerationService imageGenerationService;
    private final OpenAiService openAiService;
    private final NewsCrawlerService newsCrawlerService;
    private final VideoGenerationService videoGenerationService;

    @Operation(summary = "전체 카테고리 헤드라인 뉴스 조회", description = "네이버 뉴스의 모든 카테고리별 헤드라인 뉴스를 조회합니다.")
    @GetMapping("/news")
    public ResponseEntity<ApiResponse<List<CategoryNewsResponseDto>>> getHeadlineNews() {
        log.info("헤드라인 뉴스 조회 테스트 요청");
        try {
            List<CategoryNewsResponseDto> headlines = newsCrawlerService.crawlAllCategoryHeadlines();
            log.info("헤드라인 뉴스 조회 성공 - {} 카테고리", headlines.size());
            return ResponseEntity.ok(ApiResponse.success(headlines));
        } catch (Exception e) {
            log.error("헤드라인 뉴스 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure("헤드라인 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/script")
    @Operation(summary = "숏폼 비디오 스크립트 생성 테스트", description = "제목과 본문을 바탕으로 숏폼 비디오 스크립트를 생성합니다.")
    public ResponseEntity<ApiResponse<String>> testScriptGeneration(
            @Parameter(description = "뉴스 제목", example = "AI 기술의 발전과 미래 전망")
            @RequestParam String title,
            @Parameter(description = "뉴스 본문", example = "인공지능 기술이 빠르게 발전하면서...")
            @RequestBody String body) {

        log.info("스크립트 생성 테스트 요청 - 제목: {}", title);

        try {
            String script = openAiService.generateShortFormVideoScript(title, body).block();
            return ResponseEntity.ok(ApiResponse.success(script));
        } catch (Exception e) {
            log.error("스크립트 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure("스크립트 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/tts")
    @Operation(summary = "문장별 한국어 TTS 테스트", description = "주어진 한국어 스크립트를 문장별로 분리하여 각각 음성으로 변환한 후 ZIP 파일로 압축하여 반환합니다.")
    public ResponseEntity<byte[]> testKoreanTts(
            @Parameter(description = "문장별로 분리하여 음성으로 변환할 한국어 스크립트 (줄바꿈으로 구분)",
                    example = "안녕하세요, 첫 번째 문장입니다.\n이것은 두 번째 문장이에요.\n마지막 문장입니다.")
            @RequestParam String script) {

        log.info("문장별 한국어 TTS 테스트 요청 - 스크립트 길이: {}", script.length());

        try {
            List<byte[]> audioFiles = textToSpeechService.synthesizeSpeechKoreanBySentences(script);

            log.info("문장별 TTS 생성 완료 - {} 개의 음성 파일", audioFiles.size());

            // ZIP 파일로 압축
            byte[] zipContent = createZipFile(audioFiles);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "tts_files.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipContent);

        } catch (Exception e) {
            log.error("문장별 TTS 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] createZipFile(List<byte[]> audioFiles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < audioFiles.size(); i++) {
                ZipEntry entry = new ZipEntry("sentence_" + (i + 1) + ".mp3");
                zos.putNextEntry(entry);
                zos.write(audioFiles.get(i));
                zos.closeEntry();
            }
        }

        log.info("ZIP 파일 생성 완료 - {} 개의 TTS 파일 포함", audioFiles.size());
        return baos.toByteArray();
    }

    @GetMapping("/image")
    @Operation(summary = "문장별 이미지 시리즈 생성 테스트", description = "주어진 스크립트를 문장별로 분리하여 각각 이미지를 생성한 후 ZIP 파일로 압축하여 반환합니다.")
    public ResponseEntity<byte[]> testImageGeneration(
            @Parameter(description = "문장별로 분리하여 이미지를 생성할 스크립트 (줄바꿈으로 구분)",
                    example = "오픈AI가 서울에 지사를 연다고 해.\n다음 달 10일에 개소식을 한대.")
            @RequestParam String script) {

        log.info("문장별 이미지 생성 테스트 요청 - 스크립트 길이: {}", script.length());

        try {
            List<byte[]> imageFiles = imageGenerationService.generateImageSeriesByScript(script);

            // ZIP 파일로 압축
            byte[] zipContent = createImageZipFile(imageFiles);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "image_series.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipContent);

        } catch (Exception e) {
            log.error("문장별 이미지 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] createImageZipFile(List<byte[]> imageFiles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < imageFiles.size(); i++) {
                ZipEntry entry = new ZipEntry("image_" + (i + 1) + ".png");
                zos.putNextEntry(entry);
                zos.write(imageFiles.get(i));
                zos.closeEntry();
            }
        }

        log.info("이미지 ZIP 파일 생성 완료 - {} 개의 이미지 파일 포함", imageFiles.size());
        return baos.toByteArray();
    }

    @PostMapping("/video/generate")
    @Operation(summary = "비디오 생성", description = "텍스트 프롬프트를 사용하여 비디오를 생성합니다 (16:9 비율, Fast 모델 고정)")
    public ResponseEntity<VideoGenerationResponse> generateVideo(
            @Parameter(description = "비디오 생성을 위한 텍스트 프롬프트", example = "A serene sunset over a calm ocean with gentle waves")
            @RequestParam String prompt) {
        
        log.info("비디오 생성 요청 - 프롬프트: {}", prompt);
        
        try {
            String operationId = videoGenerationService.generateVideo(prompt).block();
            
            VideoGenerationResponse response = VideoGenerationResponse.builder()
                    .operationId(operationId)
                    .status("PROCESSING")
                    .estimatedCompletionTime(120) // 2 minutes estimate
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비디오 생성 실패", e);
            return ResponseEntity.status(500)
                    .body(VideoGenerationResponse.builder()
                            .status("ERROR")
                            .errorMessage("비디오 생성 중 오류가 발생했습니다")
                            .build());
        }
    }

    @GetMapping("/video/status")
    @Operation(summary = "비디오 생성 상태 확인", description = "작업 ID를 사용하여 비디오 생성 상태를 확인합니다")
    public ResponseEntity<VideoGenerationResponse> getVideoStatus(
            @Parameter(description = "작업 ID", example = "operations/generate_12345")
            @RequestParam String operationId) {
        
        log.info("Video status check requested for operation: {}", operationId);
        
        try {
            String status = videoGenerationService.getVideoStatus(operationId).block();
            
            VideoGenerationResponse response = VideoGenerationResponse.builder()
                    .operationId(operationId)
                    .status(status)
                    .build();
            
            if ("COMPLETED".equals(status)) {
                // 실제 비디오 URL 가져오기
                try {
                    String videoUrl = videoGenerationService.getVideoUrl(operationId).block();
                    response.setVideoUrl(videoUrl);
                } catch (Exception e) {
                    log.warn("Failed to get video URL for completed operation: {}", operationId, e);
                    // URL 가져오기 실패 시에도 COMPLETED 상태로 반환
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get video status for operation: {}", operationId, e);
            return ResponseEntity.status(500)
                    .body(VideoGenerationResponse.builder()
                            .operationId(operationId)
                            .status("ERROR")
                            .errorMessage("상태 확인 중 오류가 발생했습니다")
                            .build());
        }
    }

    @GetMapping("/video/download")
    @Operation(summary = "비디오 다운로드", description = "생성된 비디오를 다운로드합니다")
    public ResponseEntity<byte[]> downloadVideo(
            @Parameter(description = "비디오 URL")
            @RequestParam String videoUrl) {
        
        log.info("Video download requested for URL: {}", videoUrl);
        
        try {
            byte[] videoBytes = videoGenerationService.downloadVideo(videoUrl).block();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf("video/mp4"));
            headers.setContentDispositionFormData("attachment", "generated_video.mp4");
            headers.setContentLength(videoBytes.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(videoBytes);
        } catch (Exception e) {
            log.error("Failed to download video from URL: {}", videoUrl, e);
            return ResponseEntity.notFound().build();
        }
    }
}