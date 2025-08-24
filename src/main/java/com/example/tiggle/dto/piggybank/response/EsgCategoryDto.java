package com.example.tiggle.dto.piggybank.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EsgCategoryDto {
    private Long id;
    private String name;
    private String description;
    private String characterName;
}
