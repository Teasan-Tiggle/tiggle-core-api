package com.example.tiggle.service.dutchpay;

import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;

public interface DutchpayService {
    void create(String encryptedUserKey, Long creatorId, CreateDutchpayRequest req);
}
