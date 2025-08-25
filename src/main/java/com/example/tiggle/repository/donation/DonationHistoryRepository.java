package com.example.tiggle.repository.donation;

import com.example.tiggle.entity.DonationHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {

    @EntityGraph(attributePaths = {"esgCategory"})
    List<DonationHistory> findByUser_IdOrderByDonatedAtDesc(Long userId);
}
