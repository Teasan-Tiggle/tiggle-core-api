package com.ssafy.tiggle.repository.piggybank;

import com.ssafy.tiggle.entity.PiggyBank;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    @EntityGraph(attributePaths = "esgCategory")
    Optional<PiggyBank> findByOwner_Id(Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PiggyBank p " +
            "set p.currentAmount = p.currentAmount + :amount, " +
            "    p.savingCount   = p.savingCount + 1 " +
            "where p.owner.id = :userId")
    int incrementBalanceAndCount(@Param("userId") Long userId,
                                 @Param("amount") BigDecimal amount);

    List<PiggyBank> findAllByAutoSavingTrue();

    @EntityGraph(attributePaths = {"owner", "owner.university", "esgCategory"})
    List<PiggyBank> findAllByAutoDonationTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update PiggyBank p
       set p.currentAmount       = p.currentAmount - :amount,
           p.donationCount       = p.donationCount + 1,
           p.donationTotalAmount = p.donationTotalAmount + :amount
     where p.id = :piggyId
       and p.currentAmount >= :amount
    """)
    int applyDonation(@Param("piggyId") Long piggyId, @Param("amount") BigDecimal amount);
}
