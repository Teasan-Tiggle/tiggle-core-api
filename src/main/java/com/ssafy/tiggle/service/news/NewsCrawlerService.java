package com.ssafy.tiggle.service.news;

import com.ssafy.tiggle.dto.news.CategoryNewsResponseDto;
import java.util.List;

public interface NewsCrawlerService {

    /**
     * 모든 카테고리의 헤드라인 뉴스를 크롤링합니다.
     */
    List<CategoryNewsResponseDto> crawlAllCategoryHeadlines() throws Exception;
}