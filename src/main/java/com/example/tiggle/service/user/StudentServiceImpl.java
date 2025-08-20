package com.example.tiggle.service.user;

import com.example.tiggle.dto.user.JoinRequestDto;
import com.example.tiggle.entity.Department;
import com.example.tiggle.entity.Student;
import com.example.tiggle.entity.University;
import com.example.tiggle.repository.user.DepartmentRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.repository.user.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean checkDuplicateEmail(String email) {
        return studentRepository.existsByEmail(email);
    }
    @Override
    public boolean checkDuplicateStudent(Integer universityId, String studentId) {
        return studentRepository.existsByUniversityIdAndStudentId(universityId, studentId);
    }

    @Override
    public boolean joinUser(JoinRequestDto requestDto) {

        if (checkDuplicateEmail(requestDto.getEmail()) || checkDuplicateStudent(requestDto.getUniversityId(), requestDto.getStudentId())) {
            return false; // 이미 존재하는 사용자
        }

        University university = universityRepository.findById(requestDto.getUniversityId())
                .orElseThrow(() -> new RuntimeException("University not found with ID: " + requestDto.getUniversityId()));
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + requestDto.getDepartmentId()));

        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        Student student = new Student();
        student.setEmail(requestDto.getEmail());
        student.setPassword(encodedPassword);
        student.setName(requestDto.getName());
        student.setUniversity(university);
        student.setDepartment(department);
        student.setStudentId(requestDto.getStudentId());
        student.setPhone(requestDto.getPhone());

        try {
            studentRepository.save(student);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean loginUser(String email, String password) {
        return studentRepository.findByEmail(email)
                .map(foundStudent -> passwordEncoder.matches(password, foundStudent.getPassword()))
                .orElse(false);
    }
}