package com.example.tiggle.dto.finopenapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DeleteDemandDepositAccountResponse {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    @JsonProperty("REC")
    DeleteDemandDepositAccountREC rec; // 계좌 해지 결과
}