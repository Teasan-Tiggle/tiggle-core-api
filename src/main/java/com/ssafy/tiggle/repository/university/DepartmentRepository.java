package com.ssafy.tiggle.repository.university;

import com.ssafy.tiggle.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByUniversityIdOrderByNameAsc(Long universityId);
}
