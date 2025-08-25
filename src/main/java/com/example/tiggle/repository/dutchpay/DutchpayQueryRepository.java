package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.repository.dutchpay.projection.DutchpayDetailProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DutchpayQueryRepository extends Repository<Dutchpay, Long> {

    @Query(value = """
    SELECT
        d.id                           AS dutchpayId,
        d.title                        AS title,
        d.message                      AS message,
        u.name                         AS requesterName,   -- ← users.name 사용
        (SELECT COUNT(*) FROM dutchpay_share s2 WHERE s2.dutchpay_id = d.id) AS participantCount,
        d.total_amount                 AS totalAmount,
        d.created_at                   AS createdAt,
        COALESCE(s.amount, 0)          AS myAmount,
        COALESCE(d.rounded_per_person, 0) AS originalAmount,
        d.pay_more                     AS payMore,
        d.creator_id                   AS creatorId
    FROM dutchpay d
    JOIN users u ON u.id = d.creator_id                   -- ← student → users 로 변경
    LEFT JOIN dutchpay_share s ON s.dutchpay_id = d.id AND s.user_id = :userId
    WHERE d.id = :dutchpayId
""", nativeQuery = true)
    DutchpayDetailProjection findDetail(@Param("dutchpayId") Long dutchpayId,
                                        @Param("userId") Long userId);


}
