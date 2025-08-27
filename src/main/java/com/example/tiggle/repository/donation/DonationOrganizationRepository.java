package com.example.tiggle.repository.donation;

import com.example.tiggle.entity.DonationOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonationOrganizationRepository extends JpaRepository<DonationOrganization, Long> {

    Optional<DonationOrganization> findByEsgCategory_id(Long esgCategoryId);
}
