package com.ssafy.tiggle.service.university;

import com.ssafy.tiggle.dto.university.DepartmentResponseDto;
import com.ssafy.tiggle.dto.university.UniversityResponseDto;

import java.util.List;

public interface UniversityService {
    List<UniversityResponseDto> getAllUniversities();

    List<DepartmentResponseDto> getAllDepartments(Long universityId);
}
