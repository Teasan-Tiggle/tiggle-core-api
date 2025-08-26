package com.example.tiggle.service.donation;

import com.example.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.donation.request.DonationRequest;
import com.example.tiggle.dto.donation.response.DonationHistoryResponse;
import com.example.tiggle.dto.donation.response.DonationGrowthLevel;
import com.example.tiggle.dto.donation.response.DonationStatus;
import com.example.tiggle.dto.donation.response.DonationSummary;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DonationService {

    Mono<ApiResponse<PrimaryAccountInfoDto>> getDonation(Long userId, String encryptedUserKey);

    Mono<ApiResponse<Object>> createDonation(Long userId, String encryptedUserKey, DonationRequest request);

    ApiResponse<List<DonationHistoryResponse>> getDonationHistory(Long userId);

    DonationStatus getUserDonationStatus(Long userId);

    DonationStatus getUniversityDonationStatus(Long userId);

    DonationStatus getTotalDonationStatus();

    DonationGrowthLevel getDonationGrowthLevel(Long userId);

    DonationSummary getUserDonationSummary(Long userId);
}
