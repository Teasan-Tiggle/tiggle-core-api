package com.example.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireTransactionHistoryRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String accountNo; // 계좌번호
    String transactionUniqueNo; // 거래고유번호
}