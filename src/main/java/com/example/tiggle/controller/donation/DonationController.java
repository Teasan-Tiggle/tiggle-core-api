package com.example.tiggle.controller.donation;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.service.donation.DonationService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/donation")
@Tag(name = "기부 API", description = "기부 관련 API")
public class DonationController {

    private final DonationService donationService;

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
}
