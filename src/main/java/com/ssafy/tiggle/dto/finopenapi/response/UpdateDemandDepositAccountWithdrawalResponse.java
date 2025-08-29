package com.ssafy.tiggle.dto.finopenapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountWithdrawalResponse {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    @JsonProperty("REC")
    UpdateDemandDepositAccountWithdrawalREC rec; // 출금 결과 정보
}