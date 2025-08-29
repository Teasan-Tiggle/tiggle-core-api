package com.ssafy.tiggle.dto.finopenapi.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
// SSAFY 금융 API Response 공통 헤더
public class Header {
    String responseCode; // 응답 코드, 'H000'
    String responseMessage; // 응담 메시지, "정상처리 되었습니다."
    String apiName; // API 이름, 호출 API URI 뒷부분
    String transmissionDate; // API 전송일자(YYYYMMDD), 요청일
    String transmissionTime; // API 전송시각(HHMMSS), 요청시간 기준±5분
    String institutionCode; // 기관코드, '00100'로 고정
    String apiKey; // 핀테크 앱 일련번호, '001'로 고정
    String apiServiceCode; // API 서비스 코드, API 이름 필드와 동일
    String institutionTransactionUniqueNo; // 기관 거래 고유 번호, 기관별 API 서비스 호출 단위의 고유 코드
}
