package com.example.tiggle.dto.account.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "거래 내역 조회 응답 DTO")
public class TransactionHistoryResponse {
    
    @Schema(description = "거래 내역 목록")
    private List<TransactionDto> transactions;
    
    @Schema(description = "다음 페이지 커서 (없으면 null)", example = "101480")
    private String nextCursor;
    
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;
    
    @Schema(description = "총 조회된 건수", example = "20")
    private int size;
}