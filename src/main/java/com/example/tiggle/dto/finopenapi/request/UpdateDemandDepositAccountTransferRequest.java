package com.example.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountTransferRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String depositAccountNo; // 입금계좌번호
    String depositTransactionSummary; // 입금거래요약
    String transactionBalance; // 거래금액
    String withdrawalAccountNo; // 출금계좌번호
    String withdrawalTransactionSummary; // 출금거래요약
}