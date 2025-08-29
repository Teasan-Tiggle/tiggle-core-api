package com.ssafy.tiggle.dto.dutchpay.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public record DutchpayDetailResponse(
        Long id,
        Long requestUserId,   // ← 추가: 지금 요청을 보낸 유저 아이디
        String title,
        String message,
        Long totalAmount,
        String status,
        Creator creator,
        List<Share> shares,
        Long roundedPerPerson, // 유지(필요 없으면 나중에 제거 가능)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public record Creator(Long id, String name) {}
    // amount: 개인 분담 원금 / status: PENDING, PAID 등 / tiggleAmount: 낸 자투리(미납이면 0)
    public record Share(Long userId, String name, Long amount, String status, Long tiggleAmount) {}
}
