package com.example.tiggle.service.user;

import com.example.tiggle.dto.user.JoinRequestDto;

public interface StudentService {
    /**
     * 중복 이메일 여부를 체크한다.
     *
     * @param email 이메일
     * @return 중복 여부
     */
    boolean checkDuplicateEmail(String email);

    /**
     * 중복 가입 여부를 체크한다.
     *
     * @param universityId 학교 ID
     * @param studentId 학번
     * @return 중복 여부
     */
    boolean checkDuplicateStudent(Integer universityId, String studentId);

    /**
     * 회원가입한다.
     *
     * @return 회원가입 성공 여부
     */
    boolean joinUser(JoinRequestDto requestDto);
}
