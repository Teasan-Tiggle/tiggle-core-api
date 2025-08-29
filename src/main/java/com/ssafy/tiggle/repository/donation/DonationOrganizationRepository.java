package com.ssafy.tiggle.repository.donation;

import com.ssafy.tiggle.entity.DonationOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationOrganizationRepository extends JpaRepository<DonationOrganization, Long> {

    List<DonationOrganization> findByEsgCategory_id(Long esgCategoryId);
}
