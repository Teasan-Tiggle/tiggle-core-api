package com.example.tiggle.dto.dutchpay.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record DutchpayDetailResponse(
        Long id,
        String title,
        String message,
        Long totalAmount,
        String status,
        Creator creator,
        List<Share> shares,
        Long roundedPerPerson,
        Boolean payMore,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public record Creator(Long id, String name) {}
    public record Share(Long userId, String name, Long amount, String status) {}
}