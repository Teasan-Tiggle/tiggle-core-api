package com.ssafy.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireDemandDepositAccountRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String accountNo; // 계좌번호
}