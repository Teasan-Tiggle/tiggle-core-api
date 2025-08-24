package com.example.tiggle.controller.dutchpay;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.response.DutchpayCreatedResponse;
import com.example.tiggle.service.dutchpay.DutchpayService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dutchpay")
@RequiredArgsConstructor
@Tag(name = "더치페이 요청")
@SecurityRequirement(name = "bearerAuth")
public class DutchpayController {

    private final DutchpayService dutchpayService;

    @PostMapping("/requests")
    @Operation(summary = "더치페이 요청 생성(저장 + FCM 발송)")
    public ResponseEntity<ApiResponse<DutchpayCreatedResponse>> create(@Valid @RequestBody CreateDutchpayRequest req) {
        Long creatorId = JwtUtil.getCurrentUserId();
        var body = dutchpayService.create(creatorId, req);
        return ResponseEntity.ok(body);
    }
}
