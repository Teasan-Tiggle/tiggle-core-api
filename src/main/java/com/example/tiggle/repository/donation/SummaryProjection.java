package com.example.tiggle.repository.donation;

import java.math.BigDecimal;

public interface SummaryProjection {
    BigDecimal getTotalAmount();
    BigDecimal getMonthlyAmount();
    Integer getCategoryCnt();
}