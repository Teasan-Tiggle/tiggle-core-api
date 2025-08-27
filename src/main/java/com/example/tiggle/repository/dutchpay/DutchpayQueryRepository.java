package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.repository.dutchpay.projection.DutchpayDetailProjection;
import com.example.tiggle.repository.dutchpay.projection.DutchpayListItemProjection;
import com.example.tiggle.repository.dutchpay.projection.DutchpaySummaryProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DutchpayQueryRepository extends Repository<Dutchpay, Long> {

    @Query(value = """
        SELECT
            d.id                           AS dutchpayId,
            d.title                        AS title,
            d.message                      AS message,
            u.name                         AS requesterName,
            (SELECT COUNT(*) FROM dutchpay_share s2 WHERE s2.dutchpay_id = d.id) AS participantCount,
            d.total_amount                 AS totalAmount,
            d.created_at                   AS createdAt,
            COALESCE(s.amount, 0)          AS myAmount,
            COALESCE(d.rounded_per_person, 0) AS originalAmount,
            d.pay_more                     AS payMore,
            d.creator_id                   AS creatorId
        FROM dutchpay d
        JOIN users u ON u.id = d.creator_id
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
    DutchpaySummaryProjection summarizeByUserId(@Param("userId") Long userId,
                                                @Param("transferredStatus") String transferredStatus);

    /* 진행중: 예전 기준대로 d.status <> 'COMPLETED'
       키셋 기준도 예전과 동일하게 ds_u.created_at 사용 */
    @Query(value = """
        SELECT
          d.id AS dutchpayId,
          d.title AS title,
          CAST(ds_u.amount AS SIGNED) AS myAmount,
          CAST(d.total_amount AS SIGNED) AS totalAmount,
          pc.cnt AS participantCount,
          pc.paid_cnt AS paidCount,
          ds_u.created_at AS requestedAt,
          CASE WHEN d.creator_id = :userId THEN 1 ELSE 0 END AS isCreator,
          u.name AS creatorName
        FROM dutchpay d
        JOIN users u ON u.id = d.creator_id
        JOIN dutchpay_share ds_u
             ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId
        JOIN (
          SELECT dutchpay_id,
                 COUNT(*) AS cnt,
                 SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_cnt
          FROM dutchpay_share
          GROUP BY dutchpay_id
        ) pc ON pc.dutchpay_id = d.id
        WHERE d.status <> 'COMPLETED'
          AND (
                :cursorAt IS NULL
                OR ds_u.created_at < :cursorAt
                OR (ds_u.created_at = :cursorAt AND d.id < :cursorId)
              )
        ORDER BY ds_u.created_at DESC, d.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DutchpayListItemProjection> findInProgressAfter(@Param("userId") Long userId,
                                                         @Param("cursorAt") LocalDateTime cursorAt,
                                                         @Param("cursorId") Long cursorId,
                                                         @Param("limit") int limit);

    /* 완료: 예전 기준대로 d.status = 'COMPLETED'
       필요하면 아주 빡빡하게 하려면 AND pc.paid_cnt = pc.cnt 를 추가해도 됨 */
    @Query(value = """
        SELECT
          d.id AS dutchpayId,
          d.title AS title,
          CAST(ds_u.amount AS SIGNED) AS myAmount,
          CAST(d.total_amount AS SIGNED) AS totalAmount,
          pc.cnt AS participantCount,
          pc.paid_cnt AS paidCount,
          ds_u.created_at AS requestedAt,
          CASE WHEN d.creator_id = :userId THEN 1 ELSE 0 END AS isCreator,
          u.name AS creatorName
        FROM dutchpay d
        JOIN users u ON u.id = d.creator_id
        JOIN dutchpay_share ds_u
             ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId
        JOIN (
          SELECT dutchpay_id,
                 COUNT(*) AS cnt,
                 SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) AS paid_cnt
          FROM dutchpay_share
          GROUP BY dutchpay_id
        ) pc ON pc.dutchpay_id = d.id
        WHERE d.status = 'COMPLETED'
          AND (
                :cursorAt IS NULL
                OR ds_u.created_at < :cursorAt
                OR (ds_u.created_at = :cursorAt AND d.id < :cursorId)
              )
        ORDER BY ds_u.created_at DESC, d.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DutchpayListItemProjection> findCompletedAfter(@Param("userId") Long userId,
                                                        @Param("cursorAt") LocalDateTime cursorAt,
                                                        @Param("cursorId") Long cursorId,
                                                        @Param("limit") int limit);
}
