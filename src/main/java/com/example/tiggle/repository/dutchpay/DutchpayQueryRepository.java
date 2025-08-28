package com.example.tiggle.repository.dutchpay;

import com.example.tiggle.entity.Dutchpay;
import com.example.tiggle.repository.dutchpay.projection.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DutchpayQueryRepository extends Repository<Dutchpay, Long> {

    @Query(value = """
        SELECT
          d.id                 AS dutchpayId,
          d.title              AS title,
          d.message            AS message,
          d.total_amount       AS totalAmount,
          d.status             AS status,
          d.rounded_per_person AS roundedPerPerson,
          d.created_at         AS createdAt,
          u.id                 AS creatorId,
          u.name               AS creatorName
        FROM dutchpay d
        JOIN users u ON u.id = d.creator_id
        WHERE d.id = :dutchpayId
        """, nativeQuery = true)
    DutchpayDetailHeaderProjection findDetailHeader(@Param("dutchpayId") Long dutchpayId);

    @Query(value = """
        SELECT
          ds.user_id                 AS userId,
          u.name                     AS name,
          CAST(ds.amount AS SIGNED)  AS amount,
          ds.status                  AS status
        FROM dutchpay_share ds
        JOIN users u ON u.id = ds.user_id
        WHERE ds.dutchpay_id = :dutchpayId
        ORDER BY ds.id
        """, nativeQuery = true)
    List<DutchpayShareBriefProjection> findDetailShares(@Param("dutchpayId") Long dutchpayId);

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

    /* 진행중 */
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
          u.name AS creatorName,
          ds_u.status AS myStatus,
          CAST(ds_u.pay_more AS SIGNED) AS myPayMore,
          CAST(ds_u.tiggle_amount AS SIGNED) AS myTiggleAmount
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

    /* 완료 */
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
          u.name AS creatorName,
          ds_u.status AS myStatus,
          CAST(ds_u.pay_more AS SIGNED) AS myPayMore,
          CAST(ds_u.tiggle_amount AS SIGNED) AS myTiggleAmount
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

    @Query(value = """
        SELECT
          CAST(SUM(CASE WHEN t.status <> 'COMPLETED' THEN 1 ELSE 0 END) AS SIGNED) AS inProgressCount,
          CAST(SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS SIGNED)   AS completedCount
        FROM (
          SELECT d.id, d.status
          FROM dutchpay d
          JOIN dutchpay_share ds_u
               ON ds_u.dutchpay_id = d.id AND ds_u.user_id = :userId
          GROUP BY d.id, d.status
        ) t
        """, nativeQuery = true)
    DutchpayStatusCountProjection countStatusByUserId(@Param("userId") Long userId);
}
