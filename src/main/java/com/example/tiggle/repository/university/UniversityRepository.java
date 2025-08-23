package com.example.tiggle.repository.university;

import com.example.tiggle.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UniversityRepository extends JpaRepository<University, Integer> {

    List<University> findAllByOrderByNameAsc();
}
