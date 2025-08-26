package com.example.tiggle.repository.dutchpay.projection;

public interface DutchpayListItemProjection {
    Long getDutchpayId();
    String getTitle();
    Long getMyAmount();
    Long getTotalAmount();
    Integer getParticipantCount();
    Integer getPaidCount();
    java.time.LocalDateTime getRequestedAt();
    Integer getIsCreator(); // 1 or 0 (boolean은 DB-벤더 이슈 있어 정수 권장)
}
