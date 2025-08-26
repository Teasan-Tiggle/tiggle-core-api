package com.example.tiggle.controller.dutchpay;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.example.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;
import com.example.tiggle.service.dutchpay.DutchpayReadService;
import com.example.tiggle.service.dutchpay.DutchpayService;
import com.example.tiggle.service.security.EncryptionService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dutchpay/requests")
@RequiredArgsConstructor
@Tag(name = "더치페이 요청")
@SecurityRequirement(name = "bearerAuth")
public class DutchpayController {

    private final DutchpayService dutchpayService;
    private final DutchpayReadService dutchpayReadService;
    private final DutchpayReadService readService;

    @PostMapping
    @Operation(summary = "더치페이 요청 생성(저장 + FCM 발송)")
    public ResponseEntity<ResponseDto<Void>> create(
            @Parameter(name = "Authorization", in = ParameterIn.HEADER,
                    description = "Bearer {JWT}", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateDutchpayRequest req
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 인증 토큰이 필요합니다.");
        }

        final String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        final Long creatorId = JwtUtil.getCurrentUserId();

        dutchpayService.create(encryptedUserKey, creatorId, req);

        return ResponseEntity.ok(new ResponseDto<>(true));
    }

    @GetMapping("/{id}")
    @Operation(summary = "더치페이 요청 상세 페이지 조회")
    public ResponseEntity<ResponseDto<DutchpayDetailData>> getDetail(
            @Parameter(name = "Authorization", in = ParameterIn.HEADER,
                    description = "Bearer {JWT}", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long dutchpayId
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 인증 토큰이 필요합니다.");
        }

        Long userId = JwtUtil.getCurrentUserId();
        return ResponseEntity.ok(dutchpayReadService.getDetail(dutchpayId, userId));
    }

    @Operation(summary = "더치페이 Summary", description = "총 이체 금액 / 이체 횟수 / 더치페이 참여 횟수 반환")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DutchpaySummaryResponse>> getSummary(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 인증 토큰이 필요합니다.");
        }

        Long userId = JwtUtil.getCurrentUserId();
        DutchpaySummaryResponse data = readService.getSummary(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
