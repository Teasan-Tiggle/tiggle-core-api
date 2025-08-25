package com.example.tiggle.repository.donation;

import com.example.tiggle.entity.DonationOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationOrganizationRepository extends JpaRepository<DonationOrganization, Long> {
}
