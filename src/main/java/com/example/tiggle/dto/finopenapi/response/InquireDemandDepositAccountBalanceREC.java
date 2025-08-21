package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireDemandDepositAccountBalanceREC {
    String bankCode; // 은행코드
    String accountNo; // 계좌번호
    String accountBalance; // 계좌잔액
    String accountCreatedDate; // 계좌개설일
    String accountExpiryDate; // 계좌만료일
    String lastTransactionDate; // 최종거래일자
    String currency; // 통화
}