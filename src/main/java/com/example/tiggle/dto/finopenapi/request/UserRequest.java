package com.example.tiggle.dto.finopenapi.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserRequest {
    String userId; // 사용자 아이디
    String apiKey; // API 키
}