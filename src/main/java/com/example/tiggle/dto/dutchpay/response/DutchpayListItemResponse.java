package com.example.tiggle.dto.dutchpay.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DutchpayListItemResponse(
        Long dutchpayId,
        String title,
        long myAmount,
        long totalAmount,
        int participantCount,
        int paidCount,
        LocalDateTime requestedAt,
        boolean isCreator
) {}