package com.example.tiggle.domain.dutchpay.event;

import java.util.Map;

public record DutchpayCreatedEvent(
        Long dutchpayId,
        String title,
        String message,
        Long totalAmount,
        Long creatorId,
        Map<Long, Long> userShareMap,
        String encryptedUserKey
) {}
