package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.DutchpayShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DutchpayShareRepository extends JpaRepository<DutchpayShare, Long> {
    List<DutchpayShare> findByDutchpayId(Long dutchpayId);
}