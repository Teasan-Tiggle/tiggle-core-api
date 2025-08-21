package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountTransferREC {
    String transactionUniqueNo; // 거래고유번호
    String accountNo; // 계좌번호
    String transactionDate; // 거래일자
    String transactionType; // 거래유형코드
    String transactionTypeName; // 거래유형명
    String transactionAccountNo; // 거래상대방계좌번호
}