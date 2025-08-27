package com.example.tiggle.scheduler;

import com.example.tiggle.service.donation.DonationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DonationScheduler {

    private final DonationService donationService;

    public DonationScheduler(DonationService donationService) {
        this.donationService = donationService;
    }

    // 매주 일요일 20시 실행
    @Scheduled(cron = "0 0 20 * * SUN")
    public void runWeeklyDonation() {
        donationService.transferDonations();
        donationService.updateRankingCache();
    }
}