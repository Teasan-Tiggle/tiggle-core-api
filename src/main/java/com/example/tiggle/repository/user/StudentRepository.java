package com.example.tiggle.repository.user;

import com.example.tiggle.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Users, Long> {

    boolean existsByEmail(String email);

    boolean existsByUniversityIdAndStudentId(Long universityId, String studentId);

    Optional<Users> findByEmail(String email);

    @Query("select u from Users u join fetch u.university where u.id = :id")
    Optional<Users> findByIdWithUniversity(@Param("id") Long id);
}