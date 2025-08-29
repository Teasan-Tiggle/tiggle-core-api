package com.ssafy.tiggle.dto.piggybank.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePiggyBankRequest {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal targetAmount;

    private Long esgCategoryId;
}
