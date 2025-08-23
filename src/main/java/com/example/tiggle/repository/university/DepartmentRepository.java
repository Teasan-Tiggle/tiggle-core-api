package com.example.tiggle.repository.university;

import com.example.tiggle.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    List<Department> findByUniversityIdOrderByNameAsc(Long universityId);
}
