package com.example.tiggle.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(enumAsRef = true)
public enum Category {
    PLANET(1L),
    PEOPLE(2L),
    PROSPERITY(3L);

    private final Long id;

    Category(Long id) {
        this.id = id;
    }

}
