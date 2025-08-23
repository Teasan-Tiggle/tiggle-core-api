package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.UpdatePiggySettingsRequest;
import com.example.tiggle.dto.piggy.response.PiggyBankResponse;
import reactor.core.publisher.Mono;

public interface PiggyService {
    Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(Integer userId);
    Mono<ApiResponse<PiggyBankResponse>> updateSettings(Integer userId, UpdatePiggySettingsRequest req);
    Mono<ApiResponse<PiggyBankResponse>> setCategory(Integer userId, Long categoryId);
    Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Integer userId);
}

