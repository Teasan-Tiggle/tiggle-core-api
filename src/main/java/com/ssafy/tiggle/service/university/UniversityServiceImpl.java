package com.ssafy.tiggle.service.university;

import com.ssafy.tiggle.dto.university.DepartmentResponseDto;
import com.ssafy.tiggle.dto.university.UniversityResponseDto;
import com.ssafy.tiggle.repository.university.DepartmentRepository;
import com.ssafy.tiggle.repository.university.UniversityRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public List<UniversityResponseDto> getAllUniversities() {
        return universityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(u -> new UniversityResponseDto(u.getId(), u.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentResponseDto> getAllDepartments(Long universityId) {
        return departmentRepository.findByUniversityIdOrderByNameAsc(universityId)
                .stream()
                .map(d -> new DepartmentResponseDto(d.getId(), d.getName()))
                .collect(Collectors.toList());
    }
}
