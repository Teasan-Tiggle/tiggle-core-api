package com.example.tiggle.dto.finopenapi.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class CreateDemandDepositAccountRequest {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    String accountTypeUniqueNo; // 계좌 타입 고유 번호
}