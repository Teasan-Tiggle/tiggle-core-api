package com.example.tiggle.dto.donation.response;

public record DonationStatus(
        Long planetAmount,
        Long peopleAmount,
        Long prosperityAmount
) {}