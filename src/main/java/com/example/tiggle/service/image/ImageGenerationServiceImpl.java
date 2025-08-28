package com.example.tiggle.service.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final WebClient generateAiApiWebClient;

    @Override
    public byte[] generateImage(String prompt) {
        try {
            // OpenAI DALL-E API 요청 생성
            Map<String, Object> requestBody = Map.of(
                    "model", "dall-e-3",
                    "prompt", prompt,
                    "n", 1,
                    "size", "1024x1024",
                    "response_format", "b64_json"
            );

            // WebClient를 사용한 OpenAI API 호출
            Mono<Map> response = generateAiApiWebClient.post()
                    .uri("/v1/images/generations")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class);

            Map<String, Object> responseBody = response.block();

            if (responseBody != null && responseBody.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> dataArray = (java.util.List<Map<String, Object>>) responseBody.get("data");
                
                if (!dataArray.isEmpty()) {
                    String base64Image = (String) dataArray.get(0).get("b64_json");
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);

                    log.info("OpenAI DALL-E를 통한 이미지 생성이 완료되었습니다. 프롬프트: {}, 이미지 크기: {} bytes",
                            prompt, imageBytes.length);

                    return imageBytes;
                } else {
                    throw new RuntimeException("OpenAI API 응답에 이미지 데이터가 없습니다.");
                }
            } else {
                throw new RuntimeException("OpenAI API 호출에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("OpenAI DALL-E 이미지 생성에 실패했습니다. 프롬프트: {}", prompt, e);
            throw new RuntimeException("이미지 생성에 실패했습니다.", e);
        }
    }
}