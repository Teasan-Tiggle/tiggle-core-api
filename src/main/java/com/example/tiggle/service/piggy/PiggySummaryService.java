package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import reactor.core.publisher.Mono;

public interface PiggySummaryService {
    Mono<ApiResponse<PiggySummaryResponse>> getSummary(String encryptedUserKey, Integer userId);
}

