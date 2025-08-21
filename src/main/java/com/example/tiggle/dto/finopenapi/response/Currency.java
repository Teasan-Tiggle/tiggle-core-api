package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Currency {
    String currency; // 통화코드 (KRW)
    String currencyName; // 통화명 (원화)
}