package com.example.tiggle.dto.dutchpay.request;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateDutchpayRequest(
        @NotEmpty List<Long> userIds,
        @NotNull Long totalAmount,
        @NotBlank String title,
        String message,
        @NotNull Boolean creatorPaysRemainder
) { }
