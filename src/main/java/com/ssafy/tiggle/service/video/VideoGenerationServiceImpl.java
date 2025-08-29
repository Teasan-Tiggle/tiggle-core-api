package com.ssafy.tiggle.service.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationServiceImpl implements VideoGenerationService {

    @Value("${google.genai.project-id:}")
    private String projectId;

    @Value("${google.genai.location:us-central1}")
    private String location;

    @Value("${google.genai.credentials:}")
    private String credentials;

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private GoogleCredentials googleCredentials;
    private String accessToken;

    private static final String VEO_2_MODEL = "veo-2.0-generate-001";
    private static final String VEO_3_MODEL = "veo-3.0-generate-preview";
    private static final String VEO_3_FAST_MODEL = "veo-3.0-fast-generate-preview";
    
    @PostConstruct
    public void initializeVertexAI() {
        log.info("Vertex AI REST API 초기화 시작");
        
        if (projectId == null || projectId.trim().isEmpty()) {
            throw new RuntimeException("Veo 모델 사용을 위해서는 프로젝트 ID가 필요합니다. google.genai.project-id를 설정하세요.");
        }
        
        if (credentials == null || credentials.trim().isEmpty()) {
            throw new RuntimeException("Vertex AI 사용을 위해서는 서비스 계정 credentials가 필요합니다. google.genai.credentials를 설정하세요.");
        }
        
        try {
            // Google Credentials 초기화 (JSON 문자열 또는 파일 경로 지원)
            if (credentials.trim().startsWith("{")) {
                // JSON 문자열인 경우
                googleCredentials = GoogleCredentials.fromStream(
                    new java.io.ByteArrayInputStream(credentials.getBytes())
                ).createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            } else {
                // 파일 경로인 경우
                googleCredentials = GoogleCredentials.fromStream(new FileInputStream(credentials))
                        .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            }
            
            // 초기 액세스 토큰 획득
            refreshAccessToken();
            
            // Cloud Storage 버킷 확인
            ensureBucketExists();
            
            log.info("Vertex AI REST API 초기화 완료 - 프로젝트: {}, 위치: {}", projectId, location);
        } catch (IOException e) {
            log.error("Vertex AI 초기화 실패: {}", e.getMessage(), e);
            throw new RuntimeException("Vertex AI 초기화에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    private void ensureBucketExists() {
        try {
            String bucketName = "tiggle-bucket";
            String bucketUrl = String.format("https://storage.googleapis.com/storage/v1/b/%s", bucketName);
            
            // 버킷 존재 확인
            webClient.get()
                    .uri(bucketUrl)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            
            log.info("Cloud Storage 버킷 확인 완료: gs://{}", bucketName);
        } catch (Exception e) {
            log.warn("Cloud Storage 버킷 확인 실패: gs://tiggle-bucket. 버킷이 존재하는지 확인하세요. 오류: {}", e.getMessage());
        }
    }
    
    private void refreshAccessToken() throws IOException {
        googleCredentials.refresh();
        this.accessToken = googleCredentials.getAccessToken().getTokenValue();
        log.debug("액세스 토큰 갱신 완료");
    }

    @Override
    public Mono<String> generateVideo(String textPrompt) {
        return generateVideo(textPrompt, VEO_2_MODEL);
    }

    @Override
    public Mono<String> generateVideo(String textPrompt, String model) {
        return Mono.fromCallable(() -> {
            try {
                log.info("비디오 생성 시작 - 모델: {}, 프롬프트 길이: {} 문자", model, textPrompt.length());
                
                // 요청 URL 구성 (Veo 모델 Long Running API)
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning", 
                        location, projectId, location, model);
                
                // 고유한 파일명 생성 (타임스탬프 기반)
                String timestamp = String.valueOf(System.currentTimeMillis());
                String fileName = String.format("veo-video-%s.mp4", timestamp);
                String storageUri = String.format("gs://tiggle-bucket/%s", fileName);
                
                // 요청 본문 구성 (Veo 모델 정확한 구조)
                Map<String, Object> requestBody = Map.of(
                    "instances", Collections.singletonList(Map.of(
                        "prompt", textPrompt
                    )),
                    "parameters", Map.of(
                        "aspectRatio", "9:16",
                        "sampleCount", 1,
                        "durationSeconds", 5,
                        "enhancePrompt", true,
                        "storageUri", storageUri
                    )
                );
                
                // REST API 호출
                String response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(2));
                
                // 응답에서 operation ID 추출
                JsonNode responseNode = objectMapper.readTree(response);
                String operationId = responseNode.path("name").asText();
                
                log.info("비디오 생성 작업 시작 완료 - 작업 ID: {}", operationId);
                return operationId;
                
            } catch (Exception e) {
                log.error("비디오 생성 실패 - 모델: {}, 오류: {}", model, e.getMessage(), e);
                throw new RuntimeException("비디오 생성에 실패했습니다", e);
            }
        });
    }

    @Override
    public Mono<String> generateVideo(String textPrompt, String model, Integer seed) {
        return Mono.fromCallable(() -> {
            try {
                log.info("시드 포함 비디오 생성 시작 - 모델: {}, 시드: {}, 프롬프트 길이: {} 문자", 
                         model, seed, textPrompt.length());
                
                // 요청 URL 구성 (Veo 모델 Long Running API)
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning", 
                        location, projectId, location, model);
                
                // 고유한 파일명 생성 (타임스탬프 + 시드 기반)
                String timestamp = String.valueOf(System.currentTimeMillis());
                String fileName = String.format("veo-video-seed-%s-%s.mp4", seed != null ? seed : "noseed", timestamp);
                String storageUri = String.format("gs://tiggle-bucket/%s", fileName);
                
                // 요청 본문 구성 (시드 포함)
                Map<String, Object> parameters = new java.util.HashMap<>();
                parameters.put("aspectRatio", "9:16");
                parameters.put("sampleCount", 1);
                parameters.put("durationSeconds", 5);
                parameters.put("enhancePrompt", true);
                parameters.put("storageUri", storageUri);
                
                // 시드가 있으면 추가
                if (seed != null) {
                    parameters.put("seed", seed);
                }
                
                Map<String, Object> requestBody = Map.of(
                    "instances", Collections.singletonList(Map.of(
                        "prompt", textPrompt
                    )),
                    "parameters", parameters
                );
                
                // REST API 호출
                String response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(2));
                
                // 응답에서 operation ID 추출
                JsonNode responseNode = objectMapper.readTree(response);
                String operationId = responseNode.path("name").asText();
                
                log.info("시드 포함 비디오 생성 작업 시작 완료 - 작업 ID: {}, 시드: {}", operationId, seed);
                return operationId;
                
            } catch (Exception e) {
                log.error("시드 포함 비디오 생성 실패 - 모델: {}, 시드: {}, 오류: {}", 
                          model, seed, e.getMessage(), e);
                throw new RuntimeException("시드 포함 비디오 생성에 실패했습니다", e);
            }
        });
    }

    @Override
    public Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl) {
        return generateVideoSequence(textPrompt, previousVideoUrl, VEO_2_MODEL);
    }

    @Override
    public Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl, String model) {
        return generateVideoSequence(textPrompt, previousVideoUrl, model, null);
    }

    @Override
    public Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl, String model, Integer seed) {
        return Mono.fromCallable(() -> {
            try {
                log.info("연속 비디오 생성 시작 - 모델: {}, 이전 비디오: {}, 시드: {}, 프롬프트 길이: {} 문자", 
                         model, previousVideoUrl, seed, textPrompt.length());
                
                // 요청 URL 구성 (Veo 모델 Long Running API)
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:predictLongRunning", 
                        location, projectId, location, model);
                
                // 이어지는 영상의 경우 프롬프트에 연속성을 명시
                String continuationPrompt = "Continue from the previous video. " + textPrompt;
                
                // 고유한 파일명 생성 (연속 비디오용)
                String timestamp = String.valueOf(System.currentTimeMillis());
                String fileName = String.format("veo-sequence-%s-%s.mp4", seed != null ? seed : "noseed", timestamp);
                String storageUri = String.format("gs://tiggle-bucket/%s", fileName);
                
                // 요청 본문 구성 (연속 비디오)
                Map<String, Object> parameters = new java.util.HashMap<>();
                parameters.put("aspectRatio", "9:16");
                parameters.put("sampleCount", 1);
                parameters.put("durationSeconds", 5);
                parameters.put("enhancePrompt", true);
                parameters.put("storageUri", storageUri);
                
                // 시드가 있으면 추가
                if (seed != null) {
                    parameters.put("seed", seed);
                }
                
                Map<String, Object> requestBody = Map.of(
                    "instances", Collections.singletonList(Map.of(
                        "prompt", continuationPrompt
                    )),
                    "parameters", parameters
                );
                
                // REST API 호출
                String response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofMinutes(2));
                
                // 응답에서 operation ID 추출
                JsonNode responseNode = objectMapper.readTree(response);
                String operationId = responseNode.path("name").asText();
                
                log.info("연속 비디오 생성 작업 시작 완료 - 작업 ID: {}, 이전 비디오: {}", operationId, previousVideoUrl);
                return operationId;
                
            } catch (Exception e) {
                log.error("연속 비디오 생성 실패 - 모델: {}, 이전 비디오: {}, 오류: {}", 
                          model, previousVideoUrl, e.getMessage(), e);
                throw new RuntimeException("연속 비디오 생성에 실패했습니다", e);
            }
        });
    }

    @Override
    public Mono<String> getVideoStatus(String operationId){
        return getVideoStatus(operationId, VEO_2_MODEL);
    }

    private Mono<String> getVideoStatus(String operationId, String model) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("비디오 상태 확인 시작 - 작업 ID: {}", operationId);
                
                // Operation 상태 확인 URL
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:fetchPredictOperation", location, projectId, location, model);

                Map<String, Object> requestBody = Map.of("operationName", operationId);

                // REST API 호출
                String response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(30));
                
                // 응답 파싱
                JsonNode responseNode = objectMapper.readTree(response);
                
                if (responseNode.has("done") && responseNode.path("done").asBoolean()) {
                    if (responseNode.has("error")) {
                        log.error("비디오 생성 실패 - 작업 ID: {}, 오류: {}", 
                                  operationId, responseNode.path("error").toString());
                        return "FAILED";
                    }
                    log.info("비디오 생성 완료 - 작업 ID: {}", operationId);
                    return "COMPLETED";
                } else {
                    log.debug("비디오 생성 진행 중 - 작업 ID: {}", operationId);
                    return "PROCESSING";
                }
                
            } catch (Exception e) {
                log.error("비디오 상태 확인 실패 - 작업 ID: {}, 오류: {}", operationId, e.getMessage(), e);
                return "ERROR";
            }
        });
    }

    @Override
    public Mono<String> getVideoUrl(String operationId){
        return getVideoUrl(operationId, VEO_2_MODEL);
    }

    private Mono<String> getVideoUrl(String operationId,  String model) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("비디오 URL 추출 시작 - 작업 ID: {}", operationId);
                
                // Operation 상태 확인 URL
                String url = String.format("https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:fetchPredictOperation", location, projectId, location, model);

                Map<String, Object> requestBody = Map.of("operationName", operationId);
                
                // REST API 호출
                String response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(Duration.ofSeconds(30));
                
                // 응답 파싱
                JsonNode responseNode = objectMapper.readTree(response);
                
                if (responseNode.has("done") && responseNode.path("done").asBoolean() && !responseNode.has("error")) {
                    // 완료된 작업에서 비디오 URL 추출
                    if (responseNode.has("response")) {
                        JsonNode responseData = responseNode.path("response");
                        
                        if (responseData.has("videos") && responseData.path("videos").isArray()) {
                            JsonNode videosArray = responseData.path("videos");
                            if (videosArray.size() > 0) {
                                JsonNode firstVideo = videosArray.get(0);
                                if (firstVideo.has("gcsUri")) {
                                    String videoUrl = firstVideo.path("gcsUri").asText();
                                    log.info("비디오 URL 추출 완료 - 작업 ID: {}, URL: {}", operationId, videoUrl);
                                    return videoUrl;
                                }
                            }
                        }
                        
                        log.warn("예상치 못한 응답 구조 - 작업 ID: {}, 응답: {}", operationId, responseData.toString());
                        throw new RuntimeException("응답 구조에서 비디오 URL을 찾을 수 없습니다");
                    }
                    throw new RuntimeException("완료된 작업에 응답 데이터가 없습니다");
                    
                } else if (responseNode.has("error")) {
                    log.error("작업 실패로 인한 URL 추출 실패 - 작업 ID: {}, 오류: {}", 
                              operationId, responseNode.path("error").toString());
                    throw new RuntimeException("작업이 실패했습니다: " + responseNode.path("error").toString());
                } else {
                    log.warn("작업이 아직 완료되지 않음 - 작업 ID: {}", operationId);
                    throw new RuntimeException("작업이 아직 완료되지 않았습니다");
                }
                
            } catch (Exception e) {
                log.error("비디오 URL 추출 실패 - 작업 ID: {}, 오류: {}", operationId, e.getMessage(), e);
                throw new RuntimeException("비디오 URL 추출에 실패했습니다", e);
            }
        });
    }

    @Override
    public Mono<byte[]> downloadVideo(String videoUrl) {
        log.info("비디오 다운로드 시작 - URL: {}", videoUrl);

        // 1. gs:// 제거
        String pathWithoutPrefix = videoUrl.replaceFirst("^gs://", "");

        // 2. 버킷명과 나머지 경로 분리 (첫번째 슬래시 기준)
        int slashIndex = pathWithoutPrefix.indexOf('/');
        String bucket = pathWithoutPrefix.substring(0, slashIndex);
        String objectPath = pathWithoutPrefix.substring(slashIndex + 1);

        // 3. `/`만 %2F로 교체
        String encodedObjectPath = objectPath.replace("/", "%2F");

        // 4. URL 조합
        String urlString = String.format("https://storage.googleapis.com/storage/v1/b/%s/o/%s?alt=media", bucket, encodedObjectPath);
        URI uri = URI.create(urlString);  // URI 객체 생성하여 WebClient에 전달
        
        return webClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMinutes(5))
                .doOnNext(videoBytes -> log.info("비디오 다운로드 완료 - URL: {}, 크기: {} bytes", videoUrl, videoBytes.length))
                .doOnError(error -> log.error("비디오 다운로드 실패 - URL: {}, 오류: {}", videoUrl, error.getMessage(), error));
    }
}