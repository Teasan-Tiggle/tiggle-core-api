package com.example.tiggle.service.auth;

import com.example.tiggle.dto.finopenapi.response.UserResponse;
import com.example.tiggle.dto.auth.JoinRequestDto;
import com.example.tiggle.entity.Department;
import com.example.tiggle.entity.Student;
import com.example.tiggle.entity.University;
import com.example.tiggle.exception.auth.AuthException;
import com.example.tiggle.repository.university.DepartmentRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.repository.university.UniversityRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final StudentRepository studentRepository;
    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    @Override
    public boolean checkDuplicateEmail(String email) {
        return studentRepository.existsByEmail(email);
    }

    @Override
    public boolean checkDuplicateStudent(Integer universityId, String studentId) {
        return studentRepository.existsByUniversityIdAndStudentId(universityId, studentId);
    }

    @Override
    @Transactional
    public boolean joinUser(JoinRequestDto requestDto) {

        // 1. DB 중복 체크
        if (checkDuplicateEmail(requestDto.getEmail())) {
            throw AuthException.duplicateEmail();
        }
        if (checkDuplicateStudent(requestDto.getUniversityId(), requestDto.getStudentId())) {
            throw AuthException.duplicateStudent(requestDto.getUniversityId(), requestDto.getStudentId());
        }

        // 2. University 및 Department 엔티티 조회
        University university = universityRepository.findById(requestDto.getUniversityId())
                .orElseThrow(() -> AuthException.universityNotFound(requestDto.getUniversityId()));
        Department department = departmentRepository.findById(requestDto.getDepartmentId())
                .orElseThrow(() -> AuthException.departmentNotFound(requestDto.getDepartmentId()));

        // 3. 금융 API 서버 중복 체크 및 userKey 획득 또는 생성
        try {
            UserResponse externalUserResponse = financialApiService.searchUser(requestDto.getEmail()).block();
            logger.warn("금융 API 내 사용자 계정 존재: {}", requestDto.getEmail());
            throw AuthException.duplicateEmail();
        } catch (WebClientResponseException.NotFound | WebClientResponseException.BadRequest ex) {
            logger.warn("금융 API 내 사용자 계정 없음: {}", requestDto.getEmail());
        } catch (Exception e) {
            throw AuthException.externalApiFailure("외부 API 사용자 조회 중 오류가 발생했습니다.", e);
        }

        try {
            UserResponse createdUser = financialApiService.createUser(requestDto.getEmail()).block();
            if (createdUser == null || createdUser.getUserKey() == null) {
                throw AuthException.externalApiUserCreationFailure("금융 API 사용자 계정 생성 중 오류가 발생했습니다.", null);
            }
        } catch (Exception e) {
            throw AuthException.externalApiUserCreationFailure("금융 API 사용자 계정 생성 중 오류가 발생했습니다.", e);
        }

        // 4. 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(requestDto.getPassword());

        // 5. Student 엔티티 생성
        Student student = new Student();
        student.setEmail(requestDto.getEmail());
        student.setPassword(encodedPassword);
        student.setName(requestDto.getName());
        student.setUniversity(university);
        student.setDepartment(department);
        student.setStudentId(requestDto.getStudentId());
        student.setPhone(requestDto.getPhone());

        // 6. DB에 저장
        try {
            studentRepository.save(student);
            return true;
        } catch (Exception e) {
            logger.error("사용자 정보 저장 실패: {}", requestDto.getEmail(), e);
            throw AuthException.externalApiFailure("DB 사용자 정보 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Map<String, Object> loginUser(String email, String password) {
        // 1. DB에서 사용자 찾기
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(AuthException::userNotFound);

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(password, student.getPassword())) {
            throw AuthException.passwordMismatch();
        }

        // 3. 금융 API 서버에서 userKey 조회
        try {
            UserResponse userResponse = financialApiService.searchUser(email).block();
            if (userResponse == null || userResponse.getUserKey() == null) {
                throw AuthException.externalApiUserNotFound();
            }

            // 4. userKey를 암호화
            String encryptedUserKey = encryptionService.encrypt(userResponse.getUserKey());

            // 5. userId와 암호화된 userKey를 Map에 담아 반환
            Map<String, Object> result = new HashMap<>();
            result.put("userId", student.getId());
            result.put("userKey", encryptedUserKey);
            return result;

        } catch (Exception e) {
            throw AuthException.externalApiFailure(e.getMessage(), e);
        }
    }
}