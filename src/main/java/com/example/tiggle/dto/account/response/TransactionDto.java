package com.example.tiggle.dto.account.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "거래 내역 DTO")
public class TransactionDto {
    
    @Schema(description = "거래 고유번호", example = "101483")
    private String transactionId;
    
    @Schema(description = "거래 일자", example = "2025-08-22")
    private String transactionDate;
    
    @Schema(description = "거래 시간", example = "14:29:19")
    private String transactionTime;
    
    @Schema(description = "거래 구분", example = "입금", allowableValues = {"입금", "출금"})
    private String transactionType;
    
    @Schema(description = "거래 설명", example = "홍길동")
    private String description;
    
    @Schema(description = "거래 금액", example = "1000")
    private String amount;
    
    @Schema(description = "거래 후 잔액", example = "50000")
    private String balanceAfter;
}