package com.example.tiggle.dto.finopenapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class InquireDemandDepositAccountListResponse {
    @JsonProperty("Header")
    Header header; // 공통 헤더
    @JsonProperty("REC")
    List<InquireDemandDepositAccountListREC> rec; // 계좌 목록
}