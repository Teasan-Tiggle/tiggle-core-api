package com.example.tiggle.repository.dutchpay.projection;

public interface DutchpayStatusCountProjection {
    Long getInProgressCount();
    Long getCompletedCount();
}