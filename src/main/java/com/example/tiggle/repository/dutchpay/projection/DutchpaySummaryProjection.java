package com.example.tiggle.repository.dutchpay.projection;

public interface DutchpaySummaryProjection {
    Long getTotalTransferredAmount();  // 총 이체 금액(원)
    Long getTransferCount();           // 이체 횟수
    Long getParticipatedCount();       // 더치페이 참여 횟수 (distinct dutchpay_id)
}

