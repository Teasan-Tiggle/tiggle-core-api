package com.example.tiggle.service.dutchpay;

import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.example.tiggle.dto.dutchpay.response.DutchpayListResponse;
import com.example.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;

public interface DutchpayService {
    void create(String encryptedUserKey, Long creatorId, CreateDutchpayRequest req);
    DutchpayDetailData getDetail(Long dutchpayId, Long userId);
    DutchpayListResponse getDutchpayListCursor(Long userId, String tab, String cursor, Integer size);
    DutchpaySummaryResponse getSummary(String encryptedUserKey, Long userId);
}
