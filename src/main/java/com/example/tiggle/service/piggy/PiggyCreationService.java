package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import reactor.core.publisher.Mono;

public interface PiggyCreationService {
    Mono<ApiResponse<PiggySummaryResponse>> create(String encryptedUserKey, Integer userId, CreatePiggyBankRequest req);
}
