package com.example.tiggle.dto.dutchpay.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record DutchpayDetailData(
        Long dutchpayId,
        String title,
        String message,
        String requesterName,
        int participantCount,
        Long totalAmount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime requestedAt,
        Long myAmount,          // 100원 단위 올림된 "내가 내는 금액"
        Long originalAmount,    // 올림 전 금액(예: 16666)
        Long tiggleAmount,      // 티끌 = myAmount - originalAmount (예: 334)
        Boolean payMoreDefault, // 자동 기부 기본값
        Boolean creator         // 내가 생성자인지 여부
) {}
