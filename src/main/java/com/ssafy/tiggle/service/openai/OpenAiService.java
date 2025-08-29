package com.ssafy.tiggle.service.openai;

import reactor.core.publisher.Mono;

public interface OpenAiService {
    Mono<String> generateShortFormVideoScript(String title, String body);
}