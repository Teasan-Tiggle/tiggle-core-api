package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.DutchpayShare;
import com.example.tiggle.entity.DutchpayShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface DutchpayShareRepository extends JpaRepository<DutchpayShare, Long> {
    Optional<DutchpayShare> findByDutchpayIdAndUserId(Long dutchpayId, Long userId);
    long countByDutchpayIdAndStatusNot(Long dutchpayId, DutchpayShareStatus status);
}