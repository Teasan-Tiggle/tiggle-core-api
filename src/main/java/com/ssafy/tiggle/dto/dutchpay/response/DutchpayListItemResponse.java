package com.ssafy.tiggle.dto.dutchpay.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime requestedAt,
        boolean isCreator,
        String creatorName,
        long tiggleAmount
) {}