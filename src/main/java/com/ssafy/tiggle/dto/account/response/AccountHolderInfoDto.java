package com.ssafy.tiggle.dto.account.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "예금주 정보 DTO")
public class AccountHolderInfoDto {
    
    @Schema(description = "은행명", example = "신한은행")
    private String bankName;
    
    @Schema(description = "계좌번호", example = "0888315782686732")
    private String accountNo;
    
    @Schema(description = "예금주명", example = "홍길동")
    private String userName;
}