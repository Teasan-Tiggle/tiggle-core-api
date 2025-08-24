package com.example.tiggle.dto.piggybank.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePiggyBankSettingsRequest {
    @Size(max = 50)
    private String name;

    @Min(0)
    private BigDecimal targetAmount;

    private Boolean autoDonation;
    private Boolean autoSaving;

    private Long esgCategoryId;
}
