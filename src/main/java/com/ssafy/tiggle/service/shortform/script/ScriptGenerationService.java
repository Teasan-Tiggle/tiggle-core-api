package com.ssafy.tiggle.service.shortform.script;

import reactor.core.publisher.Mono;

public interface ScriptGenerationService {
    Mono<String> generateShortFormVideoScript(String title, String body);
}