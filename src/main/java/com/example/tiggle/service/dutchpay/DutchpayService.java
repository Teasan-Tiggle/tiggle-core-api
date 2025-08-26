package com.example.tiggle.service.dutchpay;

import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;

public interface DutchpayService {
    void create(String encryptedUserKey, Long creatorId, CreateDutchpayRequest req);
    DutchpayDetailData getDetail(Long dutchpayId, Long userId);
}
