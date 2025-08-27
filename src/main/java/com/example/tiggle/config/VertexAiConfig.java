package com.example.tiggle.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class VertexAiConfig {

    @Value("${google.cloud.vertex-ai.credentials}")
    private String vertexAiCredentials;

    @Value("${google.cloud.vertex-ai.project-id}")
    private String projectId;

    @Value("${google.cloud.vertex-ai.location}")
    private String location;

    @Bean
    public PredictionServiceClient predictionServiceClient() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(vertexAiCredentials.getBytes())
            );

            String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);

            PredictionServiceSettings settings = PredictionServiceSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .setEndpoint(endpoint)
                    .build();

            PredictionServiceClient client = PredictionServiceClient.create(settings);
            log.info("Google Cloud Vertex AI 클라이언트가 성공적으로 초기화되었습니다. 프로젝트: {}, 위치: {}", projectId, location);

            return client;
        } catch (IOException e) {
            log.error("Google Cloud Vertex AI 클라이언트 초기화에 실패했습니다.", e);
            throw new RuntimeException("Failed to initialize Vertex AI client", e);
        }
    }
}