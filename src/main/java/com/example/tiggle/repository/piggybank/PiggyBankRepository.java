package com.example.tiggle.repository.piggybank;

import com.example.tiggle.entity.PiggyBank;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PiggyBankRepository extends JpaRepository<PiggyBank, Long> {
    @EntityGraph(attributePaths = "esgCategory")
    Optional<PiggyBank> findByOwner_Id(Long studentId);
}
