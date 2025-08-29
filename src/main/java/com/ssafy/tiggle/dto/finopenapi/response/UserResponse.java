package com.ssafy.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserResponse {
    String userId; // 사용자 아이디
    String userName; // 사용자명
    String institutionCode; // 기관코드
    String userKey; // 사용자 키
    String created; // 생성일시
    String modified; // 수정일시
}