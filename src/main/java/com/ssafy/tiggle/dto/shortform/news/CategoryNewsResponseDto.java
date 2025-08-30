package com.ssafy.tiggle.dto.shortform.news;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryNewsResponseDto {
    private String category;
    private List<NewsArticleDto> articles;
}