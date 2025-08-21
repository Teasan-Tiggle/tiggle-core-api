package com.example.tiggle.dto.finopenapi.request;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
// SSAFY 금융 API Request 공통 헤더
public class Header {
    String apiName; // API 이름, 호출 API URI의 마지막 path 명
    String transmissionDate; // 전송일자, API 전송일자 (YYYYMMDD) 요청일
    String transmissionTime; // 전송시각, API 전송시각 (HHMMSS) 요청시간 기준 ±5분
    String institutionCode; // 기관코드, '00100'로 고정
    String fintechAppNo; // 핀테크 앱 일련번호, '001'로 고정
    String apiServiceCode; // API 서비스 코드, API 이름 필드와 동일
    String institutionTransactionUniqueNo; // 기관거래고유번호, 기관별 API 서비스 호출 단위의 고유 코드
    String apiKey; // API KEY, 앱 관리자가 발급받은 API KEY
    String userKey; // User KEY, 앱 사용자가 회원가입 할 때 발급 받은 USER KEY
}