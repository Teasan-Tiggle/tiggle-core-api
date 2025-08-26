package com.example.tiggle.repository.donation;

import java.math.BigDecimal;

public interface RankingProjection {
    String getName();
    BigDecimal getAmount();
    Integer getRank();
}