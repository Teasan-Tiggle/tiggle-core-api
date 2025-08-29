package com.ssafy.tiggle.repository.dutchpay;

import com.ssafy.tiggle.entity.DutchpayShare;
import com.ssafy.tiggle.entity.DutchpayShareStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface DutchpayShareRepository extends JpaRepository<DutchpayShare, Long> {
    Optional<DutchpayShare> findByDutchpayIdAndUserId(Long dutchpayId, Long userId);
    @Query("select count(distinct s.dutchpay.id) from DutchpayShare s where s.user.id = :userId")
    long countDistinctDutchpayByUser(@Param("userId") Long userId);
    long countByDutchpayIdAndStatusNot(Long dutchpayId, DutchpayShareStatus status);
}