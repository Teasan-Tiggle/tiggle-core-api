package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateDemandDepositAccountREC {
    String bankCode; // 은행코드
    String accountNo; // 계좌번호
    Currency currency; // 통화 정보
}