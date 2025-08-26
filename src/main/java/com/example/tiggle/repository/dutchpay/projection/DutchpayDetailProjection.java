package com.example.tiggle.repository.dutchpay.projection;

import java.time.LocalDateTime;

public interface DutchpayDetailProjection {
    Long getDutchpayId();
    String getTitle();
    String getMessage();
    String getRequesterName();
    Integer getParticipantCount();
    Long getTotalAmount();
    java.time.LocalDateTime getCreatedAt();
    Long getMyAmount();
    Long getOriginalAmount(); // rounded_per_person 별칭과 일치
    Boolean getPayMore();
    Long getCreatorId();
}
