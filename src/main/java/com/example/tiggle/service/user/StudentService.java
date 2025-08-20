package com.example.tiggle.service.user;

public interface StudentService {
    /**
     * 중복 이메일 여부를 체크한다.
     *
     * @param email 이메일
     * @return 중복 여부
     */
    boolean checkDuplicateEmail(String email);
}
