package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.PiggyEntriesPageRequest;
import com.example.tiggle.dto.piggy.response.PiggyEntriesPageResponse;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PiggySummaryService {
    Mono<ApiResponse<PiggySummaryResponse>> getSummary(String encryptedUserKey, Integer userId);
    Mono<ApiResponse<PiggyEntriesPageResponse>> getEntriesPage(String encryptedUserKey, Integer userId, PiggyEntriesPageRequest req);
}

