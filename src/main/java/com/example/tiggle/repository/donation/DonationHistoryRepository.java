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

    @Query("""
            SELECT 
                SUM(d.amount) AS totalAmount,
                SUM(CASE WHEN FUNCTION('MONTH', d.donatedAt) = FUNCTION('MONTH', CURRENT_DATE) THEN d.amount ELSE 0 END) AS monthlyAmount,
                COUNT(DISTINCT d.esgCategory.id) AS categoryCnt
            FROM DonationHistory d
            WHERE d.user.id = :userId
            """)
    SummaryProjection findDonationSummaryByUserId(Long userId);

    @Query("""
            SELECT 
                RANK() OVER (ORDER BY SUM(d.amount) DESC) AS rank
            FROM DonationHistory d
            JOIN d.user u
            GROUP BY u.university.id
            HAVING u.university.id = :universityId
            """)
    Integer findUniversityRank(@Param("universityId") Long universityId);

    @Query("""
            SELECT 
                u.university.name AS name,
                SUM(d.amount) AS amount,
                RANK() OVER (ORDER BY SUM(d.amount) DESC) AS rank
            FROM DonationHistory d
            JOIN d.user u
            GROUP BY u.university.name
            ORDER BY SUM(d.amount) DESC
            """)
    List<RankingProjection> getUniversityRanking();

    @Query("""
            SELECT 
                u.department.name AS name,
                SUM(d.amount) AS amount,
                RANK() OVER (ORDER BY SUM(d.amount) DESC) AS rank
            FROM DonationHistory d
            JOIN d.user u
            WHERE u.university.id = :universityId
            GROUP BY u.department.name
            ORDER BY SUM(d.amount) DESC
            """)
    List<RankingProjection> getDepartmentRanking(@Param("universityId") Long universityId);

    @Query("""
            SELECT 
                u.university.name AS name,
                SUM(d.amount) AS amount
            FROM DonationHistory d
            JOIN d.user u
            GROUP BY u.university.name
            """)
    List<Object[]> sumByUniversity();

    @Query("""
            SELECT 
                u.department.name AS name,
                SUM(d.amount) AS amount
            FROM DonationHistory d
            JOIN d.user u
            WHERE u.university.id = :universityId
            GROUP BY u.department.name
            """)
    List<Object[]> sumByDepartment(Long universityId);
}
