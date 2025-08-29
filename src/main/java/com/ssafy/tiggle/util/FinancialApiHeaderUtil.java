package com.ssafy.tiggle.util;

import com.ssafy.tiggle.dto.finopenapi.request.Header;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class FinancialApiHeaderUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");
    private static final Random random = new Random();
    
    public static Header createHeader(String apiName, String apiKey, String userKey) {
        LocalDateTime now = LocalDateTime.now();
        String transmissionDate = now.format(DATE_FORMATTER);
        String transmissionTime = now.format(TIME_FORMATTER);
        String randomNumber = String.format("%06d", random.nextInt(1000000));
        String institutionTransactionUniqueNo = transmissionDate + transmissionTime + randomNumber;
        
        return Header.builder()
                .apiName(apiName)
                .transmissionDate(transmissionDate)
                .transmissionTime(transmissionTime)
                .institutionCode("00100")
                .fintechAppNo("001")
                .apiServiceCode(apiName)
                .institutionTransactionUniqueNo(institutionTransactionUniqueNo)
                .apiKey(apiKey)
                .userKey(userKey)
                .build();
    }
}