package com.example.tiggle.dto.account.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주 계좌 정보 DTO")
public class PrimaryAccountInfoDto {
    
    @Schema(description = "계좌명", example = "쏠편한 입출금통장(저축예금)")
    private String accountName;
    
    @Schema(description = "계좌번호", example = "123-456-789000")
    private String accountNo;
    
    @Schema(description = "잔액", example = "100000")
    private String balance;
}