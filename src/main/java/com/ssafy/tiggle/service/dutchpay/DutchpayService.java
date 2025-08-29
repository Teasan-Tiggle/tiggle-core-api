package com.ssafy.tiggle.service.dutchpay;

import com.ssafy.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.ssafy.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.ssafy.tiggle.dto.dutchpay.response.DutchpayDetailResponse;
import com.ssafy.tiggle.dto.dutchpay.response.DutchpayListResponse;
import com.ssafy.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;

public interface DutchpayService {
    void create(String encryptedUserKey, Long creatorId, CreateDutchpayRequest req);
    DutchpayDetailResponse getDetail(Long dutchpayId, Long userId);
    DutchpayListResponse getDutchpayListCursor(Long userId, String tab, String cursor, Integer size);
    DutchpaySummaryResponse getSummary(String encryptedUserKey, Long userId);
}
