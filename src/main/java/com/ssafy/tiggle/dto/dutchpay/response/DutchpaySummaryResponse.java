package com.ssafy.tiggle.dto.dutchpay.response;

import lombok.Builder;

@Builder
public record DutchpaySummaryResponse(
        long totalTransferredAmount,
        long transferCount,
        long participatedCount,
        long inProgressCount,
        long completedCount
) {}
