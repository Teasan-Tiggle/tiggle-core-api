package com.ssafy.tiggle.config;

import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SmsProps.class)
public class SmsConfig {
    @Bean
    public DefaultMessageService messageService(SmsProps props) {
        return NurigoApp.INSTANCE.initialize(props.apiKey(), props.apiSecret(), props.baseUrl());
    }
}
