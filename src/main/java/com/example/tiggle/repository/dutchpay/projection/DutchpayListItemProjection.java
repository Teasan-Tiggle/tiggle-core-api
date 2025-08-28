package com.example.tiggle.repository.dutchpay.projection;

public interface DutchpayListItemProjection {
    Long getDutchpayId();
    String getTitle();
    Long getMyAmount();
    Long getTotalAmount();
    Integer getParticipantCount();
    Integer getPaidCount();
    java.time.LocalDateTime getRequestedAt();
    Integer getIsCreator(); // 1 or 0
    String getCreatorName();
}
