package com.example.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireTransactionHistoryListRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String accountNo; // 계좌번호
    String startDate; // 조회시작일자
    String endDate; // 조회종료일자
    String transactionType; // 거래구분
    String orderByType; // 정렬순서
}