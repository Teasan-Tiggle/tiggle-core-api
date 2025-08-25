package com.example.tiggle.dto.donation.response;

import java.math.BigDecimal;

public record DonationStatus(
        BigDecimal planetAmount,
        BigDecimal peopleAmount,
        BigDecimal prosperityAmount
) {}