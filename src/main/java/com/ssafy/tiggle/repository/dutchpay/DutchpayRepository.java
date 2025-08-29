package com.ssafy.tiggle.repository.dutchpay;

import com.ssafy.tiggle.entity.Dutchpay; // 네 엔티티 패키지에 맞춰 수정
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DutchpayRepository extends JpaRepository<Dutchpay, Long> {

    @Query("select d.payMore from Dutchpay d where d.id = :id")
    Boolean findPayMoreById(@Param("id") Long id);
}
