package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireDemandDepositAccountHolderNameREC {
    String bankCode; // 은행코드
    String bankName; // 은행명
    String accountNo; // 계좌번호
    String userName; // 사용자명
    String currency; // 통화
}