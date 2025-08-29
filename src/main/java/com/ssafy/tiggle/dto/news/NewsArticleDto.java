package com.ssafy.tiggle.dto.news;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsArticleDto {
    private String url;
    private String title;
    private String body;
}