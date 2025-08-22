package com.example.tiggle.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("티끌 API")
                        .description("'태산' 같은 변화를 만드는 금융 소셜 플랫폼 '티끌' API 문서")
                        .version("v0.1.0"))
                .components(new Components()
                        .addSecuritySchemes("encryptedUserKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("encryptedUserKey")
                                .description("암호화된 사용자 키"))
                        .addSecuritySchemes("userId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("userId")
                                .description("사용자 ID")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("encryptedUserKey")
                        .addList("userId"));
    }
}