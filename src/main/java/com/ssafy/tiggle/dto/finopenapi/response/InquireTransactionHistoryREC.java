package com.ssafy.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireTransactionHistoryREC {
    String transactionUniqueNo; // 거래고유번호
    String transactionDate; // 거래일자
    String transactionTime; // 거래시각
    String transactionType; // 거래구분
    String transactionTypeName; // 거래구분명
    String transactionAccountNo; // 거래계좌번호
    String transactionBalance; // 거래금액
    String transactionAfterBalance; // 거래후잔액
    String transactionSummary; // 거래요약
    String transactionMemo; // 거래메모
}