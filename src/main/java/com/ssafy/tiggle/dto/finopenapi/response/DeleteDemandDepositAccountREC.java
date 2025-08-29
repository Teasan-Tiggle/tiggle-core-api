package com.ssafy.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DeleteDemandDepositAccountREC {
    String status; // 계좌상태
    String accountNo; // 계좌번호
    String refundAccountNo; // 환급계좌번호
    String accountBalance; // 계좌잔액
}