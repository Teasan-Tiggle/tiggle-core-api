package com.ssafy.tiggle.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.video.GeminiVideoGenerationDto;
import com.ssafy.tiggle.dto.video.GeminiVideoStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GEMINI_VIDEO_MODEL = "veo-3.0-fast-generate-preview";
    
    public VideoGenerationServiceImpl(@Qualifier("geminiApiWebClient") WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
        log.info("Gemini Video Generation API 초기화 완료");
    }

    @Override
    public Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideo(String textPrompt) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Gemini 비디오 생성 시작 - 프롬프트 길이: {} 문자", textPrompt.length());
                
                // 요청 본문 구성
                Map<String, Object> requestBody = Map.of(
                    "instances", Collections.singletonList(Map.of(
                        "prompt", textPrompt
                    ))
                );
                
                // REST API 호출
                String response = geminiWebClient.post()
                        .uri("/models/" + GEMINI_VIDEO_MODEL + ":predictLongRunning")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(2));
                
                // 응답에서 operation name 추출
                JsonNode responseNode = objectMapper.readTree(response);
                String operationName = responseNode.path("name").asText();
                
                log.info("Gemini 비디오 생성 작업 시작 완료 - Operation Name: {}", operationName);
                GeminiVideoGenerationDto generationDto = GeminiVideoGenerationDto.of(operationName);
                return ApiResponse.success(generationDto);
                
            } catch (Exception e) {
                log.error("Gemini 비디오 생성 실패 - 오류: {}", e.getMessage(), e);
                return ApiResponse.failure("Gemini 비디오 생성에 실패했습니다: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<ApiResponse<GeminiVideoGenerationDto>> generateVideoWithImage(String textPrompt, MultipartFile imageFile) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Gemini 이미지 기반 비디오 생성 시작 - 프롬프트 길이: {} 문자, 이미지 파일명: {}, 크기: {} bytes", 
                         textPrompt.length(), imageFile.getOriginalFilename(), imageFile.getSize());
                
                // MultipartFile을 Base64로 인코딩
                String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
                
                // 요청 본문 구성 (Gemini API Image 객체 형식)
                Map<String, Object> requestBody = Map.of(
                    "instances", Collections.singletonList(Map.of(
                        "prompt", textPrompt,
                        "image", Map.of(
                            "bytesBase64Encoded", base64Image,
                            "mimeType", imageFile.getContentType()
                        )
                    ))
                );
                
                // REST API 호출
                String response = geminiWebClient.post()
                        .uri("/models/" + GEMINI_VIDEO_MODEL + ":predictLongRunning")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(2));
                
                // 응답에서 operation name 추출
                JsonNode responseNode = objectMapper.readTree(response);
                String operationName = responseNode.path("name").asText();
                
                log.info("Gemini 이미지 기반 비디오 생성 작업 시작 완료 - Operation Name: {}", operationName);
                GeminiVideoGenerationDto generationDto = GeminiVideoGenerationDto.of(operationName);
                return ApiResponse.success(generationDto);
                
            } catch (Exception e) {
                log.error("Gemini 이미지 기반 비디오 생성 실패 - 오류: {}", e.getMessage(), e);
                return ApiResponse.failure("Gemini 이미지 기반 비디오 생성에 실패했습니다: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<ApiResponse<GeminiVideoStatusDto>> getVideoStatus(String operationName) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("Gemini 비디오 상태 확인 시작 - Operation Name: {}", operationName);
                
                // REST API 호출
                String response = geminiWebClient.get()
                        .uri("/" + operationName)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(30));
                
                // 응답 파싱
                JsonNode responseNode = objectMapper.readTree(response);
                
                if (responseNode.has("done") && responseNode.path("done").asBoolean()) {
                    if (responseNode.has("error")) {
                        log.error("Gemini 비디오 생성 실패 - Operation Name: {}, 오류: {}", 
                                  operationName, responseNode.path("error").toString());
                        return ApiResponse.failure("비디오 생성이 실패했습니다: " + responseNode.path("error").toString());
                    }
                    
                    // 비디오 URI 추출
                    String videoUri = responseNode.path("response")
                            .path("generateVideoResponse")
                            .path("generatedSamples")
                            .get(0)
                            .path("video")
                            .path("uri")
                            .asText();
                    
                    log.info("Gemini 비디오 생성 완료 - Operation Name: {}, Video URI: {}", operationName, videoUri);
                    GeminiVideoStatusDto statusDto = GeminiVideoStatusDto.completed(operationName, videoUri);
                    return ApiResponse.success(statusDto);
                } else {
                    log.debug("Gemini 비디오 생성 진행 중 - Operation Name: {}", operationName);
                    GeminiVideoStatusDto statusDto = GeminiVideoStatusDto.processing(operationName);
                    return ApiResponse.success(statusDto);
                }
                
            } catch (Exception e) {
                log.error("Gemini 비디오 상태 확인 실패 - Operation Name: {}, 오류: {}", operationName, e.getMessage(), e);
                return ApiResponse.failure("비디오 상태 확인에 실패했습니다: " + e.getMessage());
            }
        });
    }

    @Override
    public Mono<byte[]> downloadVideo(String operationName) {
        return Mono.fromCallable(() -> {
            try {
                log.info("Gemini 비디오 다운로드 시작 - Operation Name: {}", operationName);
                
                // 먼저 operation 상태를 확인하여 비디오 URI를 얻음
                String response = geminiWebClient.get()
                        .uri("/" + operationName)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(30));
                
                // 응답에서 비디오 URI 추출
                JsonNode responseNode = objectMapper.readTree(response);
                
                if (!responseNode.has("done") || !responseNode.path("done").asBoolean()) {
                    throw new RuntimeException("비디오가 아직 생성되지 않았습니다");
                }
                
                if (responseNode.has("error")) {
                    throw new RuntimeException("비디오 생성이 실패했습니다: " + responseNode.path("error").toString());
                }
                
                // API 응답 구조에 따라 비디오 URI 추출
                String videoUri = responseNode.path("response")
                    .path("generateVideoResponse")
                    .path("generatedSamples")
                    .get(0)
                    .path("video")
                    .path("uri")
                    .asText();
                
                if (videoUri.isEmpty()) {
                    throw new RuntimeException("비디오 URI를 찾을 수 없습니다");
                }
                
                log.info("비디오 URI 추출 완료: {}", videoUri);
                
                // 기존 geminiWebClient를 사용하여 비디오 다운로드
                return geminiWebClient.get()
                        .uri(videoUri)
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .timeout(Duration.ofMinutes(5))
                        .doOnNext(videoBytes -> {
                            log.info("Gemini 비디오 다운로드 완료 - Operation Name: {}, 크기: {} bytes", 
                                    operationName, videoBytes.length);
                        })
                        .doOnError(error -> log.error("Gemini 비디오 다운로드 실패 - Operation Name: {}, 오류: {}", 
                                                    operationName, error.getMessage()))
                        .block();
                
            } catch (Exception e) {
                log.error("Gemini 비디오 다운로드 실패 - Operation Name: {}, 오류: {}", operationName, e.getMessage(), e);
                throw new RuntimeException("Gemini 비디오 다운로드에 실패했습니다", e);
            }
        });
    }
}