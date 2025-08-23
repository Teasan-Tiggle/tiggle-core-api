package com.example.tiggle.dto.piggy.response;

import lombok.AllArgsConstructor; import lombok.Data;
import java.math.BigDecimal; import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class PiggyEntryItemDto {
    private String id;
    private String type;           // "CHANGE" or "DUTCHPAY"
    private BigDecimal amount;
    private OffsetDateTime occurredAt;
    private String title;          // "8월의 4번째 자투리 적립"
}
