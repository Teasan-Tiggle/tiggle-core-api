package com.example.tiggle.dto.dutchpay.response;

import java.util.List;

public record DutchpayCreatedResponse(
        Long dutchpayId,
        String title,
        String message,
        Long totalAmount,
        List<Share> shares // 각 유저별 금액
) {
    public record Share(Long userId, Long amount, String status) {}
}
