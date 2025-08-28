package com.example.tiggle.service.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final WebClient generateAiApiWebClient;

    @Override
    public List<byte[]> generateImageSeriesByScript(String script) {
        try {
            log.info("이미지 시리즈 생성 시작 - 스크립트 길이: {}", script.length());

            // 줄바꿈으로 문장 분리 (TTS와 동일한 방식)
            String normalizedScript = script.replace("\\n", "\n");
            List<String> sentences = Arrays.stream(normalizedScript.split("\n"))
                    .map(String::trim)
                    .filter(sentence -> !sentence.isEmpty())
                    .collect(Collectors.toList());

            log.info("총 {} 개의 문장으로 분리됨", sentences.size());

            String fullContext = String.join(" ", sentences);

            List<byte[]> images = new java.util.ArrayList<>();
            for (int i = 0; i < sentences.size(); i++) {
                String enhancedPrompt = createEnhancedPrompt(sentences.get(i), fullContext, i + 1);
                log.debug("이미지 생성 중: {}", enhancedPrompt);
                byte[] image = generateImageWithConsistentStyle(enhancedPrompt);
                images.add(image);
            }

            log.info("이미지 시리즈 생성 완료 - {} 개의 이미지", images.size());
            return images;

        } catch (Exception e) {
            log.error("이미지 시리즈 생성에 실패했습니다. 스크립트: {}", script, e);
            throw new RuntimeException("이미지 시리즈 생성에 실패했습니다.", e);
        }
    }

    private byte[] generateImageWithConsistentStyle(String prompt) {
        try {
            // OpenAI DALL-E API 요청 생성
            Map<String, Object> requestBody = Map.of(
                    "model", "dall-e-3",
                    "prompt", prompt,
                    "n", 1,
                    "size", "1024x1024",
                    "response_format", "b64_json",
                    "style", "natural"
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
                List<Map<String, Object>> dataArray = (List<Map<String, Object>>) responseBody.get("data");

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

    private String createEnhancedPrompt(String currentSentence, String fullContext, int sentenceNumber) {
        return String.format(
                "전체 스토리: \"%s\" " +
                        "이 스토리의 %d번째 장면인 \"%s\"을 지브리와 같은 따뜻한 톤의 캐릭터 애니메이션 스타일로 그려줘. " +
                        "이미지에 절대로 텍스트, 글자, 문자, 단어를 포함하지 마. 오직 그림으로만 표현해줘.",
                fullContext, sentenceNumber, currentSentence
        );
    }
}