package com.example.tiggle.dto.dutchpay;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateDutchpayRequest(
        @NotEmpty List<Long> participantUserIds, // 생성자 제외
        @NotNull @Min(1) Long totalAmount,
        @NotBlank @Size(max=50) String title,
        @Size(max=200) String message,
        @NotNull Boolean creatorPaysRemainder
) {}
