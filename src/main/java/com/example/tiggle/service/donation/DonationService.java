package com.example.tiggle.service.donation;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import reactor.core.publisher.Mono;

public interface DonationService {
    Mono<ApiResponse<Object>> createDonation(Long userId, String encryptedUserKey, DonationRequest request);
}
