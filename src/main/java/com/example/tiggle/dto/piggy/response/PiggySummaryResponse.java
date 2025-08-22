package com.example.tiggle.dto.piggy.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PiggySummaryResponse {
    private String name;
    private BigDecimal currentAmount;
    private BigDecimal lastWeekSavedAmount;
}
