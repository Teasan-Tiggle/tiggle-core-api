package com.ssafy.tiggle.service.donation;

import com.ssafy.tiggle.dto.account.response.PrimaryAccountInfoDto;
import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.donation.request.DonationRequest;
import com.ssafy.tiggle.dto.donation.response.*;
import org.springframework.transaction.annotation.Transactional;
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

    List<DonationRanking> getUniversityRanking();

    List<DonationRanking> getDepartmentRanking(Long userId);

    @Transactional
    void transferDonations();

    @Transactional(readOnly = true)
    void updateRankingCache();

    @Transactional
    CharacterLevel useHeart(Long userId);
}
