package com.ssafy.tiggle.service.shortform.script;

import com.ssafy.tiggle.dto.common.ApiResponse;
import reactor.core.publisher.Mono;

public interface ScriptGenerationService {
    Mono<ApiResponse<String>> generateShortFormVideoScript(String title, String body);
}