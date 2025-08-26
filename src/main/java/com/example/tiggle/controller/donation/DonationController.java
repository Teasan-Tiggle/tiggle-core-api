package com.example.tiggle.controller.donation;

import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.dto.donation.response.DonationGrowthLevel;
import com.example.tiggle.dto.donation.response.DonationHistoryResponse;
import com.example.tiggle.dto.donation.response.DonationStatus;
import com.example.tiggle.dto.donation.response.DonationSummary;
import com.example.tiggle.service.donation.DonationService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/donation")
@Tag(name = "기부 API", description = "기부 관련 API")
public class DonationController {

    private final DonationService donationService;

    /**
     * 기부하기
     *
     * @return 주계좌 정보
     */
    @GetMapping()
    @Operation(summary = "기부하기", description = "기부하기 전 주계좌 정보를 반환합니다.")
    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "조회 성공"),
//            @ApiResponse(responseCode = "401", description = "인증 실패"),
//            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ApiResponse<PrimaryAccountInfoDto>> getDonation() {
        Long userId = JwtUtil.getCurrentUserId();
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        ApiResponse<PrimaryAccountInfoDto> response = donationService.getDonation(userId, encryptedUserKey).block();
        return ResponseEntity.ok(response);
    }

    /**
     * 기부하기
     *
     * @param request 기부 정보
     * @return 기부 결과
     */
    @PostMapping("")
    @Operation(summary = "기부하기", description = "주계좌에서 선택한 ESG 테마로 기부합니다.")
    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "기부 성공"),
//            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
//            @ApiResponse(responseCode = "401", description = "인증 실패"),
//            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ApiResponse<Object>> createDonation(
            @Parameter(description = "기부 정보", required = true)
            @Valid @RequestBody DonationRequest request
    ) {
        Long userId = JwtUtil.getCurrentUserId();
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        ApiResponse<Object> response = donationService.createDonation(userId, encryptedUserKey, request).block();
        return ResponseEntity.ok(response);
    }

    /**
     * 기부 기록 조회
     *
     * @return 기부 내역 목록
     */
    @GetMapping("/history")
    @Operation(summary = "나의 기부 기록", description = "나의 기부 내역을 조회합니다.")
    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "조회 성공"),
//            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
//            @ApiResponse(responseCode = "401", description = "인증 실패"),
//            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ApiResponse<List<DonationHistoryResponse>>> getDonationHistory() {
        Long userId = JwtUtil.getCurrentUserId();
        ApiResponse<List<DonationHistoryResponse>> response = donationService.getDonationHistory(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 나의 기부 현황 조회
     *
     * @return 기부 현황
     */
    @GetMapping("/status")
    @Operation(summary = "나의 기부 현황 조회", description = "나의 기부 현황을 조회합니다.")
    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "조회 성공"),
//            @ApiResponse(responseCode = "401", description = "인증 실패"),
//            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ApiResponse<DonationStatus>> getMyStatus() {
        Long userId = JwtUtil.getCurrentUserId();
        DonationStatus response = donationService.getUserDonationStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 우리 학교 기부 현황 조회
     *
     * @return 기부 현황
     */
    @GetMapping("/status/university")
    @Operation(summary = "우리 학교 기부 현황 조회", description = "우리 학교의 기부 현황을 조회합니다.")
    @ApiResponses(value = {
    })
    public ResponseEntity<ApiResponse<DonationStatus>> getUniversityStatus() {
        Long userId = JwtUtil.getCurrentUserId();
        DonationStatus response = donationService.getUniversityDonationStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 전체 학교 기부 현황 조회
     *
     * @return 기부 현황
     */
    @GetMapping("/status/university/all")
    @Operation(summary = "전체 학교 기부 현황 조회", description = "전체 학교의 기부 현황을 조회합니다.")
    @ApiResponses(value = {
    })
    public ResponseEntity<ApiResponse<DonationStatus>> getTotalStatus() {
        DonationStatus response = donationService.getTotalDonationStatus();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 나의 성장 조회
     *
     * @return 기부 현황
     */
    @GetMapping("/growth")
    @Operation(summary = "나의 성장 조회", description = "나의 기부금액과 레벨을 조회합니다.")
    @ApiResponses(value = {
    })
    public ResponseEntity<ApiResponse<DonationGrowthLevel>> getGrowth() {
        Long userId = JwtUtil.getCurrentUserId();
        DonationGrowthLevel response = donationService.getDonationGrowthLevel(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 나의 기부 현황 요약 조회
     *
     * @return 기부 현황 요약
     */
    @GetMapping("/status/summary")
    public ResponseEntity<ApiResponse<DonationSummary>> getDonationSummary() {
        Long userId = JwtUtil.getCurrentUserId();
        DonationSummary response = donationService.getUserDonationSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
