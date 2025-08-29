package com.ssafy.tiggle.repository.donation;

import java.math.BigDecimal;

public interface CategorySumProjection {
    String getCategory();
    BigDecimal getTotal();
}