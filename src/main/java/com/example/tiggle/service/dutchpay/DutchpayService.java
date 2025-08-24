package com.example.tiggle.service.dutchpay;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.dutchpay.request.CreateDutchpayRequest;
import com.example.tiggle.dto.dutchpay.response.DutchpayCreatedResponse;

public interface DutchpayService {
    ApiResponse<DutchpayCreatedResponse> create(Long creatorId, CreateDutchpayRequest req);
}
