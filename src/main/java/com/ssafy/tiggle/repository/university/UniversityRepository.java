package com.ssafy.tiggle.repository.university;

import com.ssafy.tiggle.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {

    List<University> findAllByOrderByNameAsc();
}
