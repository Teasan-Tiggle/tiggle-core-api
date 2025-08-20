package com.example.tiggle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sms")
public record SmsProps(
        String apiKey,
        String apiSecret,
        String from,
        String baseUrl,
        Integer ttlSeconds,
        Integer resendIntervalSeconds,
        Integer maxAttempts,
        Boolean mock
) {}
