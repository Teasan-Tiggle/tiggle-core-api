package com.example.tiggle.service.video;

import reactor.core.publisher.Mono;

public interface VideoGenerationService {
    Mono<String> generateVideo(String textPrompt);
    Mono<String> generateVideo(String textPrompt, String model);
    Mono<String> generateVideo(String textPrompt, String model, Integer seed);
    Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl);
    Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl, String model);
    Mono<String> generateVideoSequence(String textPrompt, String previousVideoUrl, String model, Integer seed);
    Mono<String> getVideoStatus(String operationId);
    Mono<String> getVideoUrl(String operationId);
    Mono<byte[]> downloadVideo(String videoUrl);
}