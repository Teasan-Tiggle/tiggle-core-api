package com.ssafy.tiggle.dto.piggybank.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PiggyBankSummaryResponse {
    private String name;
    private BigDecimal currentAmount;
    private BigDecimal lastWeekSavedAmount;
}
