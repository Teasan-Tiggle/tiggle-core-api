package com.ssafy.tiggle.service.shortform.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.script.VideoSectionDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoGenerationDto;
import com.ssafy.tiggle.dto.shortform.video.GeminiVideoStatusDto;
import lombok.extern.slf4j.Slf4j;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;
import ws.schild.jave.info.MultimediaInfo;
import ws.schild.jave.info.VideoInfo;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class VideoGenerationServiceImpl implements VideoGenerationService {

    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;

    private static final String GEMINI_VIDEO_MODEL = "veo-3.0-fast-generate-preview";
    
    public VideoGenerationServiceImpl(@Qualifier("geminiApiWebClient") WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
        initializeFFmpeg();
        log.info("Gemini Video Generation API 초기화 완료");
    }
    
    private void initializeFFmpeg() {
        try {
            // FFmpeg와 FFprobe 경로를 찾습니다 (시스템에 설치되어 있어야 함)
            this.ffmpeg = new FFmpeg("ffmpeg");
            this.ffprobe = new FFprobe("ffprobe");
            log.info("FFmpeg CLI Wrapper 초기화 완료");
        } catch (Exception e) {
            log.error("FFmpeg 초기화 실패 - FFmpeg가 시스템에 설치되어 있는지 확인하세요", e);
            // FFmpeg가 없어도 서비스는 작동하도록 null로 설정
            this.ffmpeg = null;
            this.ffprobe = null;
        }
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

    @Override
    public Mono<ApiResponse<byte[]>> generateFullVideoFromSections(List<VideoSectionDto> sections) {
        return Mono.fromCallable(() -> {
            try {
                log.info("풀 영상 생성 시작 - 총 {} 섹션", sections.size());
                
                if (sections.isEmpty()) {
                    return ApiResponse.failure("생성할 섹션이 없습니다.");
                }
                
                List<byte[]> sectionVideos = new ArrayList<>();
                byte[] lastFrameImage = null;
                
                // 각 섹션별로 영상 생성
                for (int i = 0; i < sections.size(); i++) {
                    VideoSectionDto section = sections.get(i);
                    log.info("섹션 {} 영상 생성 시작 - {}", section.getSectionNumber(), 
                            section.getScript().substring(0, Math.min(50, section.getScript().length())));
                    
                    byte[] sectionVideoBytes;
                    
                    if (i == 0) {
                        // 첫 번째 섹션은 텍스트 프롬프트만 사용
                        sectionVideoBytes = generateSectionVideo(section.getScript());
                    } else {
                        // 두 번째 섹션부터는 이전 영상의 마지막 프레임을 시작 이미지로 사용
                        MultipartFile lastFrameFile = createMultipartFileFromBytes(lastFrameImage, "last_frame.jpg");
                        sectionVideoBytes = generateSectionVideoWithImage(section.getScript(), lastFrameFile);
                    }
                    
                    sectionVideos.add(sectionVideoBytes);
                    log.info("섹션 {} 영상 생성 완료 - 크기: {} bytes", section.getSectionNumber(), sectionVideoBytes.length);
                    
                    // 다음 섹션을 위해 마지막 프레임 추출 (마지막 섹션은 제외)
                    if (i < sections.size() - 1) {
                        lastFrameImage = extractLastFrame(sectionVideoBytes);
                        log.info("섹션 {} 마지막 프레임 추출 완료", section.getSectionNumber());
                    }
                }
                
                // 모든 섹션 영상을 이어붙이기
                byte[] finalVideo = concatenateVideos(sectionVideos);
                log.info("풀 영상 생성 완료 - 최종 크기: {} bytes", finalVideo.length);
                
                return ApiResponse.success(finalVideo);
                
            } catch (Exception e) {
                log.error("풀 영상 생성 실패", e);
                return ApiResponse.failure("풀 영상 생성 중 오류가 발생했습니다: " + e.getMessage());
            }
        });
    }
    
    private byte[] generateSectionVideo(String textPrompt) throws Exception {
        // 텍스트 프롬프트로 영상 생성
        ApiResponse<GeminiVideoGenerationDto> response = generateVideo(textPrompt).block();
        
        if (response == null || !response.isResult()) {
            throw new RuntimeException("섹션 영상 생성 실패: " + (response != null ? response.getMessage() : "null response"));
        }
        
        String operationName = response.getData().getOperationName();
        
        // 영상 생성 완료까지 대기
        waitForVideoCompletion(operationName);
        
        // 영상 다운로드
        return downloadVideo(operationName).block();
    }
    
    private byte[] generateSectionVideoWithImage(String textPrompt, MultipartFile imageFile) throws Exception {
        // 이미지와 텍스트 프롬프트로 영상 생성
        ApiResponse<GeminiVideoGenerationDto> response = generateVideoWithImage(textPrompt, imageFile).block();
        
        if (response == null || !response.isResult()) {
            throw new RuntimeException("섹션 영상 생성 실패: " + (response != null ? response.getMessage() : "null response"));
        }
        
        String operationName = response.getData().getOperationName();
        
        // 영상 생성 완료까지 대기
        waitForVideoCompletion(operationName);
        
        // 영상 다운로드
        return downloadVideo(operationName).block();
    }
    
    private void waitForVideoCompletion(String operationName) throws Exception {
        int maxAttempts = 60; // 최대 10분 대기 (10초 * 60)
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            ApiResponse<GeminiVideoStatusDto> statusResponse = getVideoStatus(operationName).block();
            
            if (statusResponse != null && statusResponse.isResult()) {
                GeminiVideoStatusDto status = statusResponse.getData();
                
                if (status.isDone()) {
                    log.info("영상 생성 완료 - Operation: {}", operationName);
                    return;
                }
                
                log.info("영상 생성 진행 중 - Operation: {}, Done: {}, Attempt: {}/{}", 
                        operationName, status.isDone(), attempt + 1, maxAttempts);
            }
            
            // 10초 대기
            Thread.sleep(10000);
            attempt++;
        }
        
        throw new RuntimeException("영상 생성 시간 초과 - Operation: " + operationName);
    }
    
    private byte[] extractLastFrame(byte[] videoBytes) {
        File tempVideoFile = null;
        File tempImageFile = null;
        
        try {
            // 임시 파일 생성
            tempVideoFile = File.createTempFile("video_", ".mp4");
            tempImageFile = File.createTempFile("frame_", ".jpg");
            
            // 비디오 바이트를 임시 파일에 저장
            try (FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                fos.write(videoBytes);
            }
            
            // JAVE를 사용하여 비디오 정보 가져오기
            MultimediaObject videoObject = new MultimediaObject(tempVideoFile);
            MultimediaInfo videoInfo = videoObject.getInfo();
            VideoInfo video = videoInfo.getVideo();
            
            if (video == null) {
                throw new RuntimeException("비디오 정보를 가져올 수 없습니다");
            }
            
            // 비디오 길이 계산
            long durationMillis = videoInfo.getDuration();
            double lastFrameTime = Math.max(0, (durationMillis - 100) / 1000.0); // 마지막에서 100ms 전
            
            // 비디오 속성 설정
            VideoAttributes videoAttributes = new VideoAttributes();
            videoAttributes.setCodec("mjpeg"); // JPEG 형태로 추출
            videoAttributes.setSize(video.getSize());
            
            // 인코딩 속성 설정
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mjpeg");
            attrs.setOffset((float) lastFrameTime); // 마지막 프레임 시간으로 이동
            attrs.setDuration(0.1f); // 0.1초 동안만 (한 프레임)
            attrs.setVideoAttributes(videoAttributes);
            
            // JAVE 인코더로 프레임 추출
            Encoder encoder = new Encoder();
            encoder.encode(videoObject, tempImageFile, attrs);
            
            // 추출된 이미지 파일 읽기
            if (tempImageFile.exists() && tempImageFile.length() > 0) {
                byte[] imageBytes = Files.readAllBytes(tempImageFile.toPath());
                log.info("JAVE 마지막 프레임 추출 성공 - 크기: {} bytes", imageBytes.length);
                return imageBytes;
            } else {
                throw new RuntimeException("추출된 이미지 파일이 없습니다");
            }
            
        } catch (Exception e) {
            log.error("JAVE 마지막 프레임 추출 실패", e);
            throw new RuntimeException("마지막 프레임 추출에 실패했습니다: " + e.getMessage(), e);
            
        } finally {
            // 임시 파일 정리
            if (tempVideoFile != null && tempVideoFile.exists()) {
                try {
                    tempVideoFile.delete();
                } catch (Exception e) {
                    log.warn("임시 비디오 파일 삭제 실패", e);
                }
            }
            if (tempImageFile != null && tempImageFile.exists()) {
                try {
                    tempImageFile.delete();
                } catch (Exception e) {
                    log.warn("임시 이미지 파일 삭제 실패", e);
                }
            }
        }
    }
    
    private byte[] concatenateVideos(List<byte[]> videoList) {
        if (videoList.isEmpty()) {
            return new byte[0];
        }
        
        if (videoList.size() == 1) {
            log.info("단일 섹션 영상이므로 이어붙이기 없이 반환");
            return videoList.get(0);
        }
        
        if (ffmpeg == null || ffprobe == null) {
            throw new RuntimeException("FFmpeg가 초기화되지 않았습니다. FFmpeg가 시스템에 설치되어 있는지 확인하세요.");
        }
        
        List<File> tempVideoFiles = new ArrayList<>();
        File outputFile = null;
        
        try {
            log.info("FFmpeg를 사용하여 {} 개의 비디오 섹션을 연결합니다", videoList.size());
            
            // 각 비디오를 임시 파일로 저장
            for (int i = 0; i < videoList.size(); i++) {
                File tempFile = File.createTempFile("section_" + i + "_", ".mp4");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(videoList.get(i));
                }
                tempVideoFiles.add(tempFile);
                log.debug("임시 파일 생성: {} ({} bytes)", tempFile.getAbsolutePath(), videoList.get(i).length);
            }
            
            // 출력 파일 생성
            outputFile = File.createTempFile("concatenated_", ".mp4");
            
            // FFmpeg 빌더로 concatenation 작업 구성
            FFmpegBuilder builder = new FFmpegBuilder();
            
            // 모든 입력 파일 추가
            for (File inputFile : tempVideoFiles) {
                builder.addInput(inputFile.getAbsolutePath());
            }
            
            // concat 필터 구성
            StringBuilder filterComplex = new StringBuilder();
            for (int i = 0; i < tempVideoFiles.size(); i++) {
                filterComplex.append("[").append(i).append(":v]").append("[").append(i).append(":a]");
            }
            filterComplex.append("concat=n=").append(tempVideoFiles.size()).append(":v=1:a=1[v][a]");
            
            // 출력 설정 - 빌더에 복합 필터 먼저 설정
            builder.setComplexFilter(filterComplex.toString());
            
            // 출력 설정
            builder.addOutput(outputFile.getAbsolutePath())
                   .setVideoCodec("libx264")
                   .setAudioCodec("aac")
                   .addExtraArgs("-map", "[v]")
                   .addExtraArgs("-map", "[a]");
            
            // FFmpeg 실행
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();
            
            // 결과 파일 읽기
            if (outputFile.exists() && outputFile.length() > 0) {
                byte[] result = Files.readAllBytes(outputFile.toPath());
                log.info("FFmpeg 비디오 연결 완료 - 최종 크기: {} bytes", result.length);
                return result;
            } else {
                throw new RuntimeException("FFmpeg 연결 작업 실패 - 출력 파일이 생성되지 않았습니다");
            }
            
        } catch (Exception e) {
            log.error("FFmpeg 비디오 연결 실패", e);
            throw new RuntimeException("비디오 연결에 실패했습니다: " + e.getMessage(), e);
            
        } finally {
            // 임시 파일들 정리
            for (File tempFile : tempVideoFiles) {
                if (tempFile != null && tempFile.exists()) {
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        log.warn("임시 비디오 파일 삭제 실패: {}", tempFile.getName(), e);
                    }
                }
            }
            if (outputFile != null && outputFile.exists()) {
                try {
                    outputFile.delete();
                } catch (Exception e) {
                    log.warn("출력 파일 삭제 실패: {}", outputFile.getName(), e);
                }
            }
        }
    }
    
    public byte[] testVideoConcatenation(List<byte[]> videoList) {
        return concatenateVideos(videoList);
    }
    
    @Override
    public Mono<byte[]> extractVideoLastFrame(String operationName) {
        return Mono.fromCallable(() -> {
            try {
                log.info("비디오 마지막 프레임 추출 시작 - Operation: {}", operationName);
                
                // 1. 먼저 비디오 다운로드
                byte[] videoBytes = downloadVideo(operationName).block();
                
                if (videoBytes == null || videoBytes.length == 0) {
                    throw new RuntimeException("비디오 다운로드 실패 또는 빈 파일");
                }
                
                log.info("비디오 다운로드 완료 - 크기: {} bytes", videoBytes.length);
                
                // 2. 마지막 프레임 추출
                byte[] frameBytes = extractLastFrame(videoBytes);
                
                log.info("마지막 프레임 추출 완료 - 이미지 크기: {} bytes", frameBytes.length);
                
                return frameBytes;
                
            } catch (Exception e) {
                log.error("비디오 마지막 프레임 추출 실패 - Operation: {}", operationName, e);
                throw new RuntimeException("비디오 마지막 프레임 추출에 실패했습니다: " + e.getMessage(), e);
            }
        });
    }
    
    private MultipartFile createMultipartFileFromBytes(byte[] bytes, String filename) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return "image";
            }

            @Override
            public String getOriginalFilename() {
                return filename;
            }

            @Override
            public String getContentType() {
                return "image/jpeg";
            }

            @Override
            public boolean isEmpty() {
                return bytes == null || bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes == null ? 0 : bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(bytes);
                }
            }
        };
    }
}