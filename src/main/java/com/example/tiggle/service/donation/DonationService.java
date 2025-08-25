package com.example.tiggle.service.donation;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.dto.donation.response.DonationHistoryResponse;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DonationService {
    Mono<ApiResponse<Object>> createDonation(Long userId, String encryptedUserKey, DonationRequest request);

    ApiResponse<List<DonationHistoryResponse>> getDonationHistory(Long userId);
}
