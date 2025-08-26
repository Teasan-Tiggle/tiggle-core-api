package com.example.tiggle.controller.dutchpay;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.example.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;
import com.example.tiggle.service.account.AccountService;
import com.example.tiggle.service.dutchpay.DutchpayService;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Tag(name = "더치페이 요청", description = "더치페이 생성/상세/정산(결제) API")
@SecurityRequirement(name = "bearerAuth")
public class DutchpayController {

    private final DutchpayService dutchpayService;
    private final AccountService accountService;
    private final DutchpayReadService readService;

    @PostMapping
    @Operation(
            summary = "더치페이 요청 생성",
            description = """
                    더치페이를 생성하고 참가자들에게 FCM 알림을 발송합니다.
                    - 생성자는 `userIds`에 포함하지 않습니다.
                    - `payMore=true`이면 생성자가 잔액(remainder)을 부담합니다.
                    """,
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            description = "Bearer {JWT}",
                            required = true,
                            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6..."
                    )
            }
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = com.example.tiggle.dto.common.ApiResponse.class),
                    examples = @ExampleObject(
                            name = "success",
                            value = """
                                    {
                                      "result": true,
                                      "message": null,
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청 검증 실패(참가자/금액 등)",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "bad_request",
                            value = """
                                    {
                                      "result": false,
                                      "message": "총 금액이 올바르지 않습니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "unauthorized",
                            value = """
                                    {
                                      "result": false,
                                      "message": "유효한 인증 토큰이 필요합니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<Void>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "더치페이 생성 요청",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateDutchpayRequest.class),
                            examples = @ExampleObject(
                                    name = "create_request",
                                    value = """
                                            {
                                              "title": "치킨 회식",
                                              "message": "치킨/맥주 더치페이 부탁!",
                                              "totalAmount": 50000,
                                              "userIds": [2,3,4],
                                              "payMore": true
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateDutchpayRequest req
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 인증 토큰이 필요합니다.");
        }

        final String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        final Long creatorId = JwtUtil.getCurrentUserId();

        dutchpayService.create(encryptedUserKey, creatorId, req);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "더치페이 상세 조회",
            description = "요청자/참가자/개별 금액/상태 등을 조회합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            in = ParameterIn.HEADER,
                            description = "Bearer {JWT}",
                            required = true
                    ),
                    @Parameter(
                            name = "id",
                            description = "더치페이 ID",
                            required = true,
                            example = "61"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "성공",
            content = @Content(
                    mediaType = "application/json",
                    // 스키마는 DutchpayDetailData지만 실제 응답은 ApiResponse<T>로 래핑됨 → 예시로 표현
                    examples = @ExampleObject(
                            name = "detail_success",
                            value = """
                                    {
                                      "result": true,
                                      "message": null,
                                      "data": {
                                        "id": 61,
                                        "title": "치킨 회식",
                                        "message": "치킨/맥주 더치페이 부탁!",
                                        "totalAmount": 50000,
                                        "status": "REQUESTED",
                                        "creator": { "id": 1, "name": "홍길동" },
                                        "shares": [
                                          {"userId": 1, "name": "홍길동", "amount": 4444, "status": "REQUESTED"},
                                          {"userId": 2, "name": "김철수", "amount": 4444, "status": "PAID"},
                                          {"userId": 3, "name": "이영희", "amount": 4444, "status": "REQUESTED"}
                                        ],
                                        "roundedPerPerson": 4500,
                                        "payMore": true,
                                        "createdAt": "2025-08-26T12:03:00Z"
                                      }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "대상이 없음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "not_found",
                            value = """
                                    {
                                      "result": false,
                                      "message": "더치페이를 찾을 수 없습니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<DutchpayDetailData>> getDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable("id") Long dutchpayId
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효한 인증 토큰이 필요합니다.");
        }

        Long userId = JwtUtil.getCurrentUserId();
        DutchpayDetailData body = dutchpayService.getDetail(dutchpayId, userId);
        return ResponseEntity.ok(ApiResponse.success(body));
    }

    @PostMapping("/{dutchpayId}/pay")
    @Operation(
            summary = "참여자 결제(정산)",
            description = """
                    현재 로그인 사용자가 자신의 지분을 생성자에게 이체합니다.
                    - `payMore=true`면 자투리(올림) 금액을 본인 저금통으로 자동 이체합니다.
                    - 이미 PAID면 멱등 처리(변경 없음).
                    """,
            parameters = {
                    @Parameter(
                            name = "dutchpayId",
                            description = "더치페이 ID",
                            required = true,
                            example = "61"
                    )
            }
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "pay_success",
                            value = """
                        {
                          "result": true,
                          "message": null,
                          "data": null
                        }
                        """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "이체 실패(검증/금융API 등)",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "pay_bad_request",
                            value = """
                                    {
                                      "result": false,
                                      "message": "지분 이체 실패: 응답 헤더 없음",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    public ResponseEntity<ApiResponse<Void>> pay(
            @PathVariable Long dutchpayId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "정산 옵션",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PayReq.class),
                            examples = {
                                    @ExampleObject(name = "pay_more_true",  value = "{ \"payMore\": true }"),
                                    @ExampleObject(name = "pay_more_false", value = "{ \"payMore\": false }")
                            }
                    )
            )
            @RequestBody PayReq req
    ) {
        String encryptedUserKey = JwtUtil.getCurrentEncryptedUserKey();
        Long userId = JwtUtil.getCurrentUserId();

        accountService.payDutchShare(encryptedUserKey, dutchpayId, userId, req.payMore())
                .block();

        return ResponseEntity.ok(ApiResponse.success());
    }

    @Schema(name = "PayReq", description = "정산 시 옵션")
    public record PayReq(
            @Schema(description = "자투리(올림) 금액을 저금통으로 보낼지 여부", example = "true")
            boolean payMore
    ) {}


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
