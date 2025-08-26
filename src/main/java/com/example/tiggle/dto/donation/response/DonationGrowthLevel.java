package com.example.tiggle.dto.donation.response;

public record DonationGrowthLevel(
        Long totalAmount,
        Long toNextLevel,
        Integer level
) {}
