package com.example.tiggle.service.university;

import com.example.tiggle.dto.university.UniversityResponseDto;
import com.example.tiggle.repository.user.UniversityRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityRepository universityRepository;

    @Override
    public List<UniversityResponseDto> getAllUniversities() {
        return universityRepository.findAllByOrderByNameAsc()
                .stream()
                .map(u -> new UniversityResponseDto(u.getId(), u.getName()))
                .collect(Collectors.toList());
    }
}
