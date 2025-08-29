package com.ssafy.tiggle.controller.piggybank;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.piggybank.request.CreatePiggyBankRequest;
import com.ssafy.tiggle.dto.piggybank.request.PiggyBankEntriesPageRequest;
import com.ssafy.tiggle.dto.piggybank.request.UpdatePiggyBankSettingsRequest;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankResponse;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankEntriesPageResponse;
import com.ssafy.tiggle.dto.piggybank.response.PiggyBankSummaryResponse;
import com.ssafy.tiggle.service.piggybank.PiggyBankService;
import com.ssafy.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/piggybank")
@RequiredArgsConstructor
@Tag(name = "저금통 설정/조회", description = "저금통 기본 정보 및 ESG 카테고리 설정")
public class PiggyBankController {

    private final PiggyBankService piggyBankService;

    @Operation(summary = "내 저금통 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PiggyBankResponse>> getMyPiggy() {
        Long userId = JwtUtil.getCurrentUserId();
        // 서비스에서 없으면 ResponseStatusException(404) 던짐 → 전역 핸들러가 그대로 내려줌
        ApiResponse<PiggyBankResponse> body = piggyBankService.getMyPiggy(userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 설정 수정", description = "name/targetAmount/autoSaving/autoDonation/esgCategoryId(선택) 부분 수정")
    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> updateSettings(
            @Valid @RequestBody UpdatePiggyBankSettingsRequest request
    ) {
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggyBankService.updateSettings(userId, request).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ESG 카테고리 설정")
    @PutMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> setCategory(@PathVariable Long categoryId) {
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggyBankService.setCategory(userId, categoryId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "ESG 카테고리 해제")
    @DeleteMapping("/category")
    public ResponseEntity<ApiResponse<PiggyBankResponse>> unsetCategory() {
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankResponse> body = piggyBankService.unsetCategory(userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 정보 조회(Summary)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PiggyBankSummaryResponse>> getSummary() {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankSummaryResponse> body = piggyBankService.getSummary(encryptedUserKey, userId).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 계좌 개설")
    @PostMapping
    public ResponseEntity<ApiResponse<PiggyBankSummaryResponse>> createPiggy(
            @Valid @RequestBody CreatePiggyBankRequest request
    ) {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankSummaryResponse> body = piggyBankService.create(encryptedUserKey, userId, request).block();
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "저금통 적립 내역 조회(커서 페이지네이션)")
    @PostMapping("/entries")
    public ResponseEntity<ApiResponse<PiggyBankEntriesPageResponse>> getEntriesPage(
            @Valid @RequestBody PiggyBankEntriesPageRequest request
    ) {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<PiggyBankEntriesPageResponse> body =
                piggyBankService.getEntriesPage(encryptedUserKey, userId, request).block();
        return ResponseEntity.ok(body);
    }
}
