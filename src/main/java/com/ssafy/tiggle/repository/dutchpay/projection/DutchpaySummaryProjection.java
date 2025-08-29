package com.ssafy.tiggle.repository.dutchpay.projection;

public interface DutchpaySummaryProjection {
    Long getTotalTransferredAmount();
    Long getTransferCount();
    Long getParticipatedCount();
}

