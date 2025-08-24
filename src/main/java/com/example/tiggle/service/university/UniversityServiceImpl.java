package com.example.tiggle.service.university;

import com.example.tiggle.dto.university.DepartmentResponseDto;
import com.example.tiggle.dto.university.UniversityResponseDto;
import com.example.tiggle.repository.university.DepartmentRepository;
import com.example.tiggle.repository.university.UniversityRepository;
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
