package com.example.tiggle.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class TextToSpeechConfig {

    @Value("${google.cloud.text-to-speech.credentials}")
    private String ttsCredentials;

    @Value("${google.cloud.text-to-speech.project-id}")
    private String projectId;

    @Bean
    public TextToSpeechClient textToSpeechClient() {
        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(ttsCredentials.getBytes())
            );

            TextToSpeechSettings settings = TextToSpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> credentials)
                    .build();

            TextToSpeechClient client = TextToSpeechClient.create(settings);
            log.info("Google Cloud Text-to-Speech 클라이언트가 성공적으로 초기화되었습니다. 프로젝트: {}", projectId);

            return client;
        } catch (IOException e) {
            log.error("Google Cloud Text-to-Speech 클라이언트 초기화에 실패했습니다.", e);
            throw new RuntimeException("Failed to initialize TTS client", e);
        }
    }
}