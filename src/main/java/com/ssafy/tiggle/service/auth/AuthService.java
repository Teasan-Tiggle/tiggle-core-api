package com.ssafy.tiggle.service.auth;

import com.ssafy.tiggle.dto.auth.JoinRequestDto;

import java.util.Map;

public interface AuthService {
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
    boolean checkDuplicateStudent(Long universityId, String studentId);

    /**
     * 회원가입한다.
     *
     */
    void joinUser(JoinRequestDto requestDto);

    /**
     * 로그인한다.
     *
     * @return 로그인 성공 시 userId(int)와 userKey(String)를 담은 Map, 실패 시 예외 발생
     */
    Map<String, Object> loginUser(String email, String password);
}