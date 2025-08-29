package com.ssafy.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class InquireDemandDepositAccountListRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
}