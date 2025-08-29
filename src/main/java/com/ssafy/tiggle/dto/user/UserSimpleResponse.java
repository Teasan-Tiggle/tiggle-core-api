package com.ssafy.tiggle.dto.user;

public record UserSimpleResponse(
        Long id,
        String name,
        String university,
        String department
) {}
