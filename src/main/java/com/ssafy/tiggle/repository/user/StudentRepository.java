package com.ssafy.tiggle.repository.user;

import com.ssafy.tiggle.entity.Users;
import com.ssafy.tiggle.repository.user.projection.UserBankLinkProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    boolean existsByUniversityIdAndStudentId(Long universityId, String studentId);

    Optional<Users> findByEmail(String email);

    @Query("select u from Users u join fetch u.university where u.id = :id")
    Optional<Users> findByIdWithUniversity(@Param("id") Long id);

    @Query("""
           select u.userKey as userKey, u.primaryAccountNo as primaryAccountNo
             from Users u
            where u.id = :userId
           """)
    Optional<UserBankLinkProjection> findBankLinkById(@Param("userId") Long userId);

    @Query("""
        select u from Users u
        left join fetch u.university univ
        left join fetch u.department dept
        where u.id <> :excludeId
        order by u.name asc
    """)
    List<Users> findAllWithSchoolAndDeptExcluding(@Param("excludeId") Long excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Users u
       set u.donationReady = true
     where u.id = :userId
       and u.donationReady = false
       and exists (
           select 1
             from PiggyBank p
            where p.owner.id = :userId
              and p.autoDonation = true
              and coalesce(p.targetAmount, 0) > 0
              and coalesce(p.currentAmount, 0) >= coalesce(p.targetAmount, 0)
       )
    """)
    int markDonationReadyIfReached(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Users u set u.donationReady = false where u.id = :userId and u.donationReady = true")
    int acquireDonationSlot(@Param("userId") Long userId);
}