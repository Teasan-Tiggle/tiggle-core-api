package com.example.tiggle.controller.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.PiggyEntriesPageRequest;
import com.example.tiggle.dto.piggy.response.PiggyEntriesPageResponse;
import com.example.tiggle.dto.piggy.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggy.request.UpdatePiggySettingsRequest;
import com.example.tiggle.dto.piggy.response.PiggyBankResponse;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import com.example.tiggle.service.piggy.PiggyCreationService;
import com.example.tiggle.service.piggy.PiggyService;
import com.example.tiggle.service.piggy.PiggySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/piggy")
@RequiredArgsConstructor
@Tag(name = "저금통 설정/조회", description = "저금통 기본 정보 및 ESG 카테고리 설정")
public class PiggyController {

    private final PiggyService piggySettingsService;
    private final PiggySummaryService piggySummaryService;
    private final PiggyCreationService piggyCreationService;

    @Operation(summary = "내 저금통 조회")
    @GetMapping
    public Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId
    ) {
        return piggySettingsService.getMyPiggy(userId);
    }

    @Operation(summary = "저금통 설정 수정", description = "name/targetAmount/autoSaving/autoDonation/esgCategoryId(선택) 부분 수정")
    @PatchMapping("/settings")
    public Mono<ApiResponse<PiggyBankResponse>> updateSettings(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId,
            @RequestBody UpdatePiggySettingsRequest request
    ) {
        return piggySettingsService.updateSettings(userId, request);
    }

    @Operation(summary = "ESG 카테고리 설정")
    @PutMapping("/category/{categoryId}")
    public Mono<ApiResponse<PiggyBankResponse>> setCategory(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId,
            @PathVariable Long categoryId
    ) {
        return piggySettingsService.setCategory(userId, categoryId);
    }

    @Operation(summary = "ESG 카테고리 해제")
    @DeleteMapping("/category")
    public Mono<ApiResponse<PiggyBankResponse>> unsetCategory(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId
    ) {
        return piggySettingsService.unsetCategory(userId);
    }

    @Operation(summary = "저금통 정보 조회(Summary)")
    @GetMapping("/summary")
    public Mono<ApiResponse<PiggySummaryResponse>> getSummary(
            @Parameter(description = "암호화된 사용자 키", required = true)
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId
    ) {
        return piggySummaryService.getSummary(encryptedUserKey, userId);
    }

    @Operation(summary = "저금통 계좌 개설")
    @PostMapping
    public Mono<ApiResponse<PiggySummaryResponse>> createPiggy(
            @Parameter(description = "암호화된 사용자 키", required = true)
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("userId") Integer userId,
            @Valid @RequestBody CreatePiggyBankRequest request
    ) {
        return piggyCreationService.create(encryptedUserKey, userId, request);
    }

    @Operation(summary = "저금통 내역 조회(커서 페이지네이션)")
    @PostMapping("/detail/entries")
    public Mono<ApiResponse<PiggyEntriesPageResponse>> getEntriesPage(
            @RequestHeader("encryptedUserKey") String encryptedUserKey,
            @RequestHeader("userId") Integer userId,
            @RequestBody PiggyEntriesPageRequest request
    ) {
        return piggySummaryService.getEntriesPage(encryptedUserKey, userId, request);
    }
}
