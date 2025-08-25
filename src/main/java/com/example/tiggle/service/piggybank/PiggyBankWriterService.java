package com.example.tiggle.service.piggybank;

import com.example.tiggle.repository.piggybank.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PiggyBankWriterService {

    private final PiggyBankRepository piggyBankRepository;

    @Transactional
    public void applyTiggle(Long userId, BigDecimal amount) {
        piggyBankRepository.incrementBalanceAndCount(userId, amount);
    }
}
