package com.ssafy.tiggle.service.piggybank;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.piggybank.request.CreatePiggyBankRequest;
import com.ssafy.tiggle.dto.piggybank.request.PiggyBankEntriesPageRequest;
import com.ssafy.tiggle.dto.piggybank.request.UpdatePiggyBankSettingsRequest;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankEntriesPageResponse;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankResponse;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankSummaryResponse;
import reactor.core.publisher.Mono;

public interface PiggyBankService {
    Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(Long userId);
    Mono<ApiResponse<PiggyBankResponse>> updateSettings(Long userId, UpdatePiggyBankSettingsRequest req);
    Mono<ApiResponse<PiggyBankResponse>> setCategory(Long userId, Long categoryId);
    Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Long userId);
    Mono<ApiResponse<PiggyBankSummaryResponse>> create(String encryptedUserKey, Long userId, CreatePiggyBankRequest req);
    Mono<ApiResponse<PiggyBankSummaryResponse>> getSummary(String encryptedUserKey, Long userId);
    Mono<ApiResponse<PiggyBankEntriesPageResponse>> getEntriesPage(String encryptedUserKey, Long userId, PiggyBankEntriesPageRequest req);
}

