package com.ssafy.tiggle.dto.piggybank.response;

import lombok.AllArgsConstructor; import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PiggyBankEntryItemDto {
    private String id;
    private String type;           // "CHANGE" or "DUTCHPAY"
    private BigDecimal amount;
    private String occurredDate;
    private String title;          // "8월의 4번째 자투리 적립"
}
