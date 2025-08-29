package com.ssafy.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class TransactionHistoryListRec {
    String totalCount; // 조회총건수
    List<InquireTransactionHistoryListREC> list; // 거래내역 목록
}