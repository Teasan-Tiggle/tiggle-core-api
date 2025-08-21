package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountWithdrawalREC {
    String transactionUniqueNo; // 거래고유번호
    String transactionDate; // 거래일자
}