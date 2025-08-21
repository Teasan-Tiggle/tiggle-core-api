package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireDemandDepositAccountListREC {
    String bankCode; // 은행코드
    String bankName; // 은행명
    String userName; // 사용자명
    String accountNo; // 계좌번호
    String accountName; // 계좌명
    String accountTypeCode; // 계좌타입코드
    String accountTypeName; // 계좌타입명
    String accountCreatedDate; // 계좌개설일
    String accountExpiryDate; // 계좌만료일
    String dailyTransferLimit; // 일일이체한도
    String oneTimeTransferLimit; // 1회이체한도
    String accountBalance; // 계좌잔액
    String lastTransactionDate; // 최종거래일자
    String currency; // 통화
}