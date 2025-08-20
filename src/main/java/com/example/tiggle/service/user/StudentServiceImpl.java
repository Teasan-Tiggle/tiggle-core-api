package com.example.tiggle.service.user;

import com.example.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    public boolean checkDuplicateEmail(String email) {
        return studentRepository.existsByEmail(email);
    }
}