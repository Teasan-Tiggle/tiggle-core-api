package com.example.tiggle.service.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class OpenAiServiceImpl implements OpenAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiServiceImpl.class);
    private final WebClient openAiWebClient;

    @Value("${external-api.openai.model}")
    private String model;

    public OpenAiServiceImpl(WebClient generateAiApiWebClient) {
        this.openAiWebClient = generateAiApiWebClient;
    }

    @Override
    public Mono<String> generateShortFormVideoScript(String title, String body) {
        logger.info("숏폼 영상 스크립트 생성 시작 - title: {}", title);

        String prompt = createShortFormVideoPrompt(title, body);
        return generateResponse(prompt)
                .doOnNext(script -> logger.info("숏폼 영상 스크립트 생성 완료 - length: {}", script.length()));
    }

    private Mono<String> generateResponse(String prompt) {
        logger.info("OpenAI API 호출 시작 - prompt: {}", prompt);

        Object requestBody = createRequestBody(prompt);
        logger.debug("Request body: {}", requestBody);

        return openAiWebClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> {
                            if (throwable instanceof WebClientResponseException.TooManyRequests) {
                                logger.warn("OpenAI API 사용량 제한 - 재시도 중...");
                                return true;
                            }
                            return false;
                        })
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("OpenAI API 재시도 횟수 초과");
                            return new RuntimeException("OpenAI API 사용량 제한으로 인해 요청 실패", retrySignal.failure());
                        }))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException.TooManyRequests) {
                        logger.error("OpenAI API 사용량 제한 초과 - 나중에 다시 시도해주세요");
                    } else {
                        logger.error("OpenAI API 호출 실패", error);
                    }
                })
                .doOnNext(response -> logger.debug("OpenAI API 응답: {}", response))
                .map(response -> {
                    if (response.choices() == null || response.choices().length == 0) {
                        logger.error("OpenAI API 응답에 choices가 없습니다");
                        throw new RuntimeException("잘못된 OpenAI API 응답");
                    }
                    String content = response.choices()[0].message().content();
                    logger.info("OpenAI API 호출 완료 - response length: {}", content.length());
                    return content;
                })
                .onErrorMap(error -> {
                    if (error instanceof WebClientResponseException.TooManyRequests) {
                        return new RuntimeException("OpenAI API 사용량 제한 - 잠시 후 다시 시도해주세요", error);
                    }
                    return error;
                });
    }

    private String createShortFormVideoPrompt(String title, String body) {
        return String.format("""
            다음 뉴스 기사를 바탕으로 대학생들이 이해하기 쉬운 30초 내외의 숏폼 영상에 사용할 스크립트를 작성해주세요.
            
            **중요한 제약사항:**
            - 원본 뉴스의 핵심 내용과 사실관계를 반드시 유지해야 합니다
            - 표현 방식과 설명 방법은 각색할 수 있지만, 내용이 완전히 달라지면 안 됩니다
            - 잘못된 정보나 과장된 내용을 추가하지 마세요
            
            **요구사항:**
            1. 실제 친구에게 설명하는 것처럼 매우 친근하고 캐주얼한 말투 사용 (반말, 줄임말, 신조어 등 적극 활용)
            2. 30초 분량 (약 150-200자 내외)  
            3. 핵심 정보만 간단명료하게 전달
            4. 흥미를 끌 수 있는 시작과 마무리
            5. 어려운 용어나 전문용어는 괄호 설명 없이 쉬운 일상 언어로 자연스럽게 풀어서 설명
            6. 원본 뉴스의 사실관계와 핵심 메시지 유지
            7. 괄호나 특수문자 사용 금지 (STT 음성 변환을 위해)
            
            **뉴스 정보:**
            제목: %s
            내용: %s
            
            흥미로운 시작과 마무리가 포함된 자연스러운 줄글 형태의 스크립트만 작성해주세요:
            """, title, body);
    }

    private Object createRequestBody(String prompt) {
        OpenAiRequest request = new OpenAiRequest(
                model,
                new OpenAiMessage[]{
                        new OpenAiMessage("user", prompt)
                },
                1000,
                0.7
        );
        logger.debug("생성된 요청 - model: {}, max_tokens: {}, temperature: {}",
                model, 1000, 0.7);
        return request;
    }

    private record OpenAiRequest(
            String model,
            OpenAiMessage[] messages,
            int max_tokens,
            double temperature
    ) {}

    private record OpenAiMessage(
            String role,
            String content
    ) {}

    private record OpenAiResponse(
            String id,
            String object,
            long created,
            String model,
            OpenAiChoice[] choices
    ) {}

    private record OpenAiChoice(
            int index,
            OpenAiMessage message,
            String finish_reason
    ) {}
}