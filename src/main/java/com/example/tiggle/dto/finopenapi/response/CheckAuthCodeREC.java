package com.example.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CheckAuthCodeREC {
    String status; // 인증상태
    String transactionUniqueNo; // 거래고유번호
    String accountNo; // 계좌번호
}