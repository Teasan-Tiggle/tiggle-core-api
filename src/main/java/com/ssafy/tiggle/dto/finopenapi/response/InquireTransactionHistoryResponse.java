package com.ssafy.tiggle.dto.finopenapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireTransactionHistoryResponse {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    @JsonProperty("REC")
    InquireTransactionHistoryREC rec; // 거래내역 상세정보
}