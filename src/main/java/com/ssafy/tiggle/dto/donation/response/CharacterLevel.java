package com.ssafy.tiggle.dto.donation.response;

public record CharacterLevel(

        Long experiencePoints,
        Long toNextLevel,
        Integer level,
        Integer heart
) {}
