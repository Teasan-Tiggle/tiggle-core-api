package com.example.tiggle.controller.test;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.news.CategoryNewsResponseDto;
import com.example.tiggle.service.image.ImageGenerationService;
import com.example.tiggle.service.news.NewsCrawlerService;
import com.example.tiggle.service.openai.OpenAiService;
import com.example.tiggle.service.tts.TextToSpeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/script")
    @Operation(summary = "숏폼 비디오 스크립트 생성 테스트", description = "제목과 본문을 바탕으로 숏폼 비디오 스크립트를 생성합니다.")
    public ResponseEntity<ApiResponse<String>> testScriptGeneration(
            @Parameter(description = "뉴스 제목", example = "AI 기술의 발전과 미래 전망")
            @RequestParam String title,
            @Parameter(description = "뉴스 본문", example = "인공지능 기술이 빠르게 발전하면서...")
            @RequestParam String body) {

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
    @Operation(summary = "한국어 TTS 테스트", description = "주어진 한국어 텍스트를 음성으로 변환합니다.")
    public ResponseEntity<byte[]> testKoreanTts(
            @Parameter(description = "음성으로 변환할 한국어 텍스트", example = "안녕하세요, 테스트입니다.")
            @RequestParam String text) {

        log.info("한국어 TTS 테스트 요청: {}", text);

        byte[] audioContent = textToSpeechService.synthesizeSpeechKorean(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentDispositionFormData("attachment", "korean_tts.mp3");

        return ResponseEntity.ok()
                .headers(headers)
                .body(audioContent);
    }

    @GetMapping("/image")
    @Operation(summary = "이미지 생성 테스트", description = "주어진 프롬프트로 이미지를 생성합니다.")
    public ResponseEntity<byte[]> testImageGeneration(
            @Parameter(description = "이미지 생성 프롬프트", example = "귀여운 고양이가 공원에서 뛰어노는 모습")
            @RequestParam String prompt) {

        log.info("이미지 생성 테스트 요청: {}", prompt);

        byte[] imageContent = imageGenerationService.generateImage(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "generated_image.png");

        return ResponseEntity.ok()
                .headers(headers)
                .body(imageContent);
    }
}