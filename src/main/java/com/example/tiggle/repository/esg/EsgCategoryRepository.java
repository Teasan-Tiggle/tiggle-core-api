package com.example.tiggle.repository.esg;

import com.example.tiggle.entity.EsgCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsgCategoryRepository extends JpaRepository<EsgCategory, Long> {
    Optional<EsgCategory> findByName(String name);
}
