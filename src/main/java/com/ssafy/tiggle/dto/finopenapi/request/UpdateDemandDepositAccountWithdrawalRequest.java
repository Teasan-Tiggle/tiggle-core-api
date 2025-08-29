package com.ssafy.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountWithdrawalRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String accountNo; // 계좌번호
    String transactionBalance; // 거래금액
    String transactionSummary; // 거래요약
}