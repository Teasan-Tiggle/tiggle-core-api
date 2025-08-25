package com.example.tiggle.repository.dutchpay.projection;

import java.time.LocalDateTime;

public interface DutchpayDetailProjection {
    Long getDutchpayId();
    String getTitle();
    String getMessage();
    String getRequesterName();
    Integer getParticipantCount();
    Long getTotalAmount();
    LocalDateTime getCreatedAt();
    Long getMyAmount();          // 내 몫(올림 전)
    Long getOriginalAmount();    // 있으면 사용, 없으면 myAmount로 대체
    Boolean getPayMore();
    Long getCreatorId();
}
