package com.ssafy.tiggle.service.piggybank;

import com.ssafy.tiggle.repository.piggybank.PiggyBankRepository;
import com.ssafy.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiggyBankWriterService {

    private final PiggyBankRepository piggyBankRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public boolean applyTiggle(Long userId, BigDecimal amount) {
        if (userId == null) throw new IllegalArgumentException("userId is null");
        if (amount == null || amount.signum() <= 0) {
            log.debug("[Piggy] skip applyTiggle: non-positive amount={}, userId={}", amount, userId);
            return false;
        }

        // 1) 저금통 증가 (없거나 경합이면 0)
        int updated = piggyBankRepository.incrementBalanceAndCount(userId, amount);
        if (updated == 0) {
            log.warn("[Piggy] incrementBalanceAndCount affected 0 rows (userId={}, amount={})", userId, amount);
            return false;
        }

        // 2) 목표 달성 시 플래그 ON (멱등)
        int flagged = studentRepository.markDonationReadyIfReached(userId);
        boolean readyNow = flagged > 0; // 1 이상이면 이번에 ON 됨(이미 ON이면 0일 수 있음)

        log.info("[Piggy] applied tiggle amount={}, userId={}, donationReadyNow={}", amount, userId, readyNow);
        return readyNow;
    }
}
