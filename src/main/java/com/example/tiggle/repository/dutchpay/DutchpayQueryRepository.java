package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.repository.dutchpay.projection.DutchpayDetailProjection;
import com.example.tiggle.repository.dutchpay.projection.DutchpayListItemProjection;
import com.example.tiggle.repository.dutchpay.projection.DutchpaySummaryProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

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


    @Query(value = """
        SELECT
          CAST(COALESCE(SUM(IF(ds.status = :transferredStatus, ds.amount, 0)), 0) AS SIGNED) AS totalTransferredAmount,
          CAST(SUM(IF(ds.status = :transferredStatus, 1, 0)) AS SIGNED)                     AS transferCount,
          CAST(COUNT(DISTINCT ds.dutchpay_id) AS SIGNED)                                     AS participatedCount
        FROM dutchpay_share ds
        WHERE ds.user_id = :userId
        """, nativeQuery = true)
    DutchpaySummaryProjection summarizeByUserId(
            @Param("userId") Long userId,
            @Param("transferredStatus") String transferredStatus
    );

    // 진행중: dutchpay.status != 'COMPLETED'
    @Query(value = """
        SELECT
          d.id                                         AS dutchpayId,
          d.title                                      AS title,
          CAST(ds_u.amount AS SIGNED)                  AS myAmount,
          CAST(d.total_amount AS SIGNED)               AS totalAmount,
          (SELECT COUNT(*) FROM dutchpay_share x WHERE x.dutchpay_id = d.id)                                       AS participantCount,
          (SELECT COUNT(*) FROM dutchpay_share x WHERE x.dutchpay_id = d.id AND x.status = 'PAID')                 AS paidCount,
          ds_u.created_at                              AS requestedAt,
          CASE WHEN d.creator_id = :userId THEN 1 ELSE 0 END                                                       AS isCreator
        FROM dutchpay d
        JOIN dutchpay_share ds_u ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId
        WHERE d.status <> 'COMPLETED'
        ORDER BY ds_u.created_at DESC
        """, nativeQuery = true)
    List<DutchpayListItemProjection> findInProgressByUser(@Param("userId") Long userId, Pageable pageable);

    // 완료: dutchpay.status = 'COMPLETED'
    @Query(value = """
        SELECT
          d.id                                         AS dutchpayId,
          d.title                                      AS title,
          CAST(ds_u.amount AS SIGNED)                  AS myAmount,
          CAST(d.total_amount AS SIGNED)               AS totalAmount,
          (SELECT COUNT(*) FROM dutchpay_share x WHERE x.dutchpay_id = d.id)                                       AS participantCount,
          (SELECT COUNT(*) FROM dutchpay_share x WHERE x.dutchpay_id = d.id AND x.status = 'PAID')                 AS paidCount,
          ds_u.created_at                              AS requestedAt,
          CASE WHEN d.creator_id = :userId THEN 1 ELSE 0 END                                                       AS isCreator
        FROM dutchpay d
        JOIN dutchpay_share ds_u ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId
        WHERE d.status = 'COMPLETED'
        ORDER BY ds_u.created_at DESC
        """, nativeQuery = true)
    List<DutchpayListItemProjection> findCompletedByUser(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT COUNT(DISTINCT d.id) FROM dutchpay d JOIN dutchpay_share ds_u ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId WHERE d.status = 'COMPLETED'", nativeQuery = true)
    long countCompleted(@Param("userId") Long userId);
}
