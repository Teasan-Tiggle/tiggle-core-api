package com.example.tiggle.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {
    @Value("${server.url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("티끌 API")
                        .description("‘태산’ 같은 변화를 만드는 금융 소셜 플랫폼 '티끌' API 문서")
                        .version("v0.1.0"))
                .servers(Arrays.asList(
                        new Server().url(serverUrl).description("개발 서버"),
                        new Server().url("https://").description("운영 서버")));
    }
}