package com.example.tiggle.repository.donation;

import com.example.tiggle.entity.DonationHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {

    @EntityGraph(attributePaths = {"esgCategory"})
    List<DonationHistory> findByUser_IdOrderByDonatedAtDesc(Long userId);

    @Query("SELECT d.esgCategory.name AS category, SUM(d.amount) AS total " +
            "FROM DonationHistory d " +
            "GROUP BY d.esgCategory.name")
    List<CategorySumProjection> findTotalAmountByCategory();

    @Query("SELECT d.esgCategory.name AS category, SUM(d.amount) AS total " +
            "FROM DonationHistory d " +
            "WHERE d.user.id = :userId " +
            "GROUP BY d.esgCategory.name")
    List<CategorySumProjection> findTotalAmountByCategoryAndUser(@Param("userId") Long userId);

    @Query("SELECT d.esgCategory.name AS category, SUM(d.amount) AS total " +
            "FROM DonationHistory d " +
            "WHERE d.user.university.id = :univId " +
            "GROUP BY d.esgCategory.name")
    List<CategorySumProjection> findTotalAmountByCategoryAndUniversity(@Param("univId") Long univId);

    @Query("SELECT SUM(d.amount) " +
            "FROM DonationHistory d " +
            "WHERE d.user.id = :userId")
    BigDecimal findTotalAmountByUserId(@Param("userId") Long userId);
}
