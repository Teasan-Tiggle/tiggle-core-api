package com.ssafy.tiggle.service.shortform.script;

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
public class ScriptGenerationServiceImpl implements ScriptGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ScriptGenerationServiceImpl.class);
    private final WebClient openAiWebClient;

    @Value("${external-api.openai.model}")
    private String model;

    public ScriptGenerationServiceImpl(WebClient generateAiApiWebClient) {
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
            다음 뉴스 기사를 바탕으로 대학생들이 쉽게 이해할 수 있는 뉴스 정보 전달용 지브리 스타일 애니메이션 숏폼 영상 스크립트를 작성해주세요.
            
            **핵심 목적: 뉴스 정보를 재미있고 이해하기 쉽게 전달**
            - 이는 일반적인 엔터테인먼트 영상이 아닌 교육적이고 정보적인 뉴스 콘텐츠입니다
            - 대학생들이 어려운 뉴스를 친근한 방식으로 접할 수 있도록 도와주는 것이 목표입니다
            
            **중요한 제약사항:**
            - 원본 뉴스의 핵심 내용과 사실관계를 반드시 유지해야 합니다
            - 표현 방식과 설명 방법은 각색할 수 있지만, 내용이 완전히 달라지면 안 됩니다
            - 잘못된 정보나 과장된 내용을 추가하지 마세요
            - 뉴스임을 명확히 알 수 있는 정보 전달 방식을 유지해야 합니다
            
            **영상 스타일 요구사항:**
            1. 지브리 스타일의 부드럽고 따뜻한 애니메이션으로 제작
            2. 전체 스크립트를 3-4개의 8초 섹션으로 나누어 작성
            3. 각 섹션마다 동일한 시각적 요소(캐릭터, 배경, 분위기)를 반복 설명하여 일관성 유지
            4. 각 섹션은 8초 분량의 대화로 구성 (실제 말하는 시간 기준)
            5. 구체적인 대화 형식: "남자1이 '...'라고 말했다", "여자1이 '...'라고 대답했다" 스타일
            
            **시각적 요소 설정:**
            - 모든 섹션에서 공통: 지브리 스타일의 부드럽고 따뜻한 애니메이션
            - 기사 내용과 분위기에 맞는 적절한 배경, 캐릭터, 의상, 소품 등을 설정
            - 한 번 설정된 시각적 요소는 모든 섹션에서 일관되게 반복 사용
            - 뉴스 주제에 따라 적절한 장소와 캐릭터 스타일 결정
            
            **뉴스 정보 전달 요구사항:**
            1. 자연스럽고 친근한 정보 공유 방식:
               - 친구에게 유용한 정보를 알려주는 듯한 자연스러운 톤
               - 형식적인 뉴스 표현 대신 일상 대화체 사용
               - "이런 일이 있었어", "이거 알아두면 좋을 것 같아"
            
            2. 정보의 교육적 가치 강조:
               - 단순 재미보다는 유익한 정보 제공에 중점
               - "이런 변화가 생겼어", "이게 중요한 이유는"
               - 대학생들에게 도움이 되는 정보임을 자연스럽게 전달
            
            **내용 구성:**
            - 섹션 1: 뉴스의 핵심 사실과 출처 소개 (언제, 어디서, 누가 발표했는지)
            - 섹션 2: 구체적인 세부 내용이나 배경 설명 (왜, 어떻게)
            - 섹션 3: 이 뉴스가 대학생들에게 미치는 영향이나 의미
            - 섹션 4 (선택): 앞으로의 전망이나 알아두면 좋을 추가 정보
            
            **뉴스 정보:**
            제목: %s
            내용: %s
            
            **출력 형식:**
            각 섹션을 "섹션 1:", "섹션 2:" 형태로 구분하고, 각 섹션마다 완전한 시각 묘사와 8초 분량의 대화를 포함해주세요.
            
            예시 형식:
            섹션 1: 지브리 스타일의 부드러운 애니메이션으로, 따뜻한 자연광이 들어오는 아늑한 카페에서 갈색 머리에 안경을 쓴 30대 남성이 베이지색 스웨터를 입고 나무 테이블에 앉아 "오픈AI가 서울에 새로운 지사를 연다고 발표했어"라고 차분하게 말한다.
            
            섹션 2: 같은 지브리 스타일 애니메이션으로, 동일한 따뜻한 카페에서 검은 머리의 20대 여성이 하늘색 블라우스를 입고 호기심 어린 표정으로 "언제부터 운영을 시작하는 거야?"라고 궁금해하며 묻는다.
            
            주의사항:
            - 각 섹션의 시각적 요소는 일관성을 위해 반드시 반복해서 설명
            - 8초 분량의 자연스러운 대화로 구성
            - 전체 스토리가 자연스럽게 이어지도록 구성
            - 대화는 친근하고 이해하기 쉬운 말투 사용
            - 괄호나 특수문자 사용 금지
            
            실제 영상 제작에 사용할 스크립트만 출력해주세요:
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