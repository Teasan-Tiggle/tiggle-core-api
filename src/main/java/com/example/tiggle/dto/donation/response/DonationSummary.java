package com.example.tiggle.dto.donation.response;

public record DonationSummary(
        Long totalAmount,       // 총 기부 금액
        Long monthlyAmount,     // 이번 달 기부 금액
        Integer categoryCnt,    // 참여 분야 수
        Integer universityRank  // 학교 순위
) {}
