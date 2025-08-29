package com.ssafy.tiggle.service.shortform.script;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.script.VideoSectionDto;
import reactor.core.publisher.Mono;
import java.util.List;

public interface ScriptGenerationService {
    Mono<ApiResponse<List<VideoSectionDto>>> generateShortFormVideoScript(String title, String body);
}