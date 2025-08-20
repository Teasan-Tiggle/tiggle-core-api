package com.example.tiggle.repository.user;

import com.example.tiggle.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

    boolean existsByEmail(String email);

    boolean existsByUniversityIdAndStudentId(Integer universityId, String studentId);

    Optional<Student> findByEmail(String email);
}