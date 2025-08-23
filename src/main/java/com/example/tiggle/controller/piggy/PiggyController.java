package com.example.tiggle.controller.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggy.request.PiggyEntriesPageRequest;
import com.example.tiggle.dto.piggy.request.UpdatePiggySettingsRequest;
import com.example.tiggle.dto.piggy.response.PiggyBankResponse;
import com.example.tiggle.dto.piggy.response.PiggyEntriesPageResponse;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import com.example.tiggle.service.piggy.PiggyCreationService;
import com.example.tiggle.service.piggy.PiggyService;
import com.example.tiggle.service.piggy.PiggySummaryService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<PiggyBankResponse>> getMyPiggy() {
        Integer userId = JwtUtil.getCurrentUserId();
        // 서비스에서 없으면 ResponseStatusException(404) 던짐 → 전역 핸들러가 그대로 내려줌
        ApiResponse<PiggyBankResponse> body = piggySettingsService.getMyPiggy(userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 설정 수정", description = "name/targetAmount/autoSaving/autoDonation/esgCategoryId(선택) 부분 수정")
    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> updateSettings(
            @Valid @RequestBody UpdatePiggySettingsRequest request
    ) {
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggySettingsService.updateSettings(userId, request).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ESG 카테고리 설정")
    @PutMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> setCategory(@PathVariable Long categoryId) {
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggySettingsService.setCategory(userId, categoryId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ESG 카테고리 해제")
    @DeleteMapping("/category")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> unsetCategory() {
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggySettingsService.unsetCategory(userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 정보 조회(Summary)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PiggySummaryResponse>> getSummary() {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggySummaryResponse> body = piggySummaryService.getSummary(encryptedUserKey, userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 계좌 개설")
    @PostMapping
    public ResponseEntity<ApiResponse<PiggySummaryResponse>> createPiggy(
            @Valid @RequestBody CreatePiggyBankRequest request
    ) {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggySummaryResponse> body = piggyCreationService.create(encryptedUserKey, userId, request).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 적립 내역 조회(커서 페이지네이션)")
    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<PiggyEntriesPageResponse>> getEntriesPage(
            @Valid @RequestBody PiggyEntriesPageRequest request
    ) {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Integer userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyEntriesPageResponse> body =
                piggySummaryService.getEntriesPage(encryptedUserKey, userId, request).block();
        return ResponseEntity.ok(body);
    }
}
