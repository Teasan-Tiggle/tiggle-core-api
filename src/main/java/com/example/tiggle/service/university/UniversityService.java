package com.example.tiggle.service.university;

import com.example.tiggle.dto.university.DepartmentResponseDto;
import com.example.tiggle.dto.university.UniversityResponseDto;

import java.util.List;

public interface UniversityService {
    List<UniversityResponseDto> getAllUniversities();

    List<DepartmentResponseDto> getAllDepartments(Long universityId);
}
