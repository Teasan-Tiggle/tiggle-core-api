package com.ssafy.tiggle.repository.esg;

import com.ssafy.tiggle.entity.EsgCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EsgCategoryRepository extends JpaRepository<EsgCategory, Long> {
    Optional<EsgCategory> findByName(String name);
}
