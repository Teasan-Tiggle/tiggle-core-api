package com.example.tiggle.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials:}")
    private String firebaseCredentials;

    @PostConstruct
    public void initialize() {
        try {
            GoogleCredentials credentials;
            
            // 환경변수에서 Firebase 인증 정보 사용
            credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(firebaseCredentials.getBytes())
            );
            log.info("Firebase initialized with environment credentials");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase application has been initialized");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase", e);
        }
    }
}