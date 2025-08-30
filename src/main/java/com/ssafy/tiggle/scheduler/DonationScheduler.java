package com.ssafy.tiggle.scheduler;

import com.ssafy.tiggle.exception.GlobalExceptionHandler;
import com.ssafy.tiggle.service.donation.DonationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DonationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final DonationService donationService;

    public DonationScheduler(DonationService donationService) {
        this.donationService = donationService;
    }

    // 매주 일요일 20시 실행
    @Scheduled(cron = "0 0 6 * * MON")
    public void runWeeklyDonation() {
        logger.info("학교 -> 단체 기부 스케줄러");
        donationService.transferDonations();
        donationService.updateRankingCache();
    }
}