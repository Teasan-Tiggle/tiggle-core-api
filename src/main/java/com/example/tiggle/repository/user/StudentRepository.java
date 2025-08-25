package com.example.tiggle.repository.user;

import com.example.tiggle.entity.Users;
import com.example.tiggle.repository.user.projection.UserBankLinkProjection;
import org.springframework.data.jpa.repository.JpaRepository;
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
}