package com.example.tiggle.service.piggybank;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggybank.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggybank.request.PiggyBankEntriesPageRequest;
import com.example.tiggle.dto.piggybank.request.UpdatePiggyBankSettingsRequest;
import com.example.tiggle.dto.piggybank.response.PiggyBankEntriesPageResponse;
import com.example.tiggle.dto.piggybank.response.PiggyBankResponse;
import com.example.tiggle.dto.piggybank.response.PiggyBankSummaryResponse;
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

