package com.example.tiggle.dto.piggybank.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PiggyBankResponse {
    private Long id;
    private String name;
    private BigDecimal currentAmount;
    private BigDecimal targetAmount;
    private Integer savingCount;
    private Integer donationCount;
    private BigDecimal donationTotalAmount;
    private Boolean autoDonation;
    private Boolean autoSaving;
    private EsgCategoryDto esgCategory;
}
