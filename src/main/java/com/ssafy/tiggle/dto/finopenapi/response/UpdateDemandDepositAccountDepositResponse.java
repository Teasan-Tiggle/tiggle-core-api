package com.ssafy.tiggle.dto.finopenapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateDemandDepositAccountDepositResponse {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    @JsonProperty("REC")
    UpdateDemandDepositAccountDepositREC rec; // 입금 결과 정보
}