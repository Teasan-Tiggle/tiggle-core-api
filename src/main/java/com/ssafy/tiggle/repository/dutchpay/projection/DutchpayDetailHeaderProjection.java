package com.ssafy.tiggle.repository.dutchpay.projection;

import java.time.LocalDateTime;

public interface DutchpayDetailHeaderProjection {
    Long getDutchpayId();
    String getTitle();
    String getMessage();
    Long getTotalAmount();
    String getStatus();
    Long getRoundedPerPerson();
    LocalDateTime getCreatedAt();
    Long getCreatorId();
    String getCreatorName();
}