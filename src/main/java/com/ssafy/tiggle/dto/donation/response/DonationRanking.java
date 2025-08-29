package com.ssafy.tiggle.dto.donation.response;

import java.math.BigDecimal;

public record DonationRanking(
        Integer rank,
        String name,
        BigDecimal amount
) {}