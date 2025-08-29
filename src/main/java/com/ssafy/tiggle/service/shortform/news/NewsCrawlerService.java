package com.ssafy.tiggle.service.shortform.news;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.news.CategoryNewsResponseDto;
import reactor.core.publisher.Mono;
import java.util.List;

public interface NewsCrawlerService {

    /**
     * 모든 카테고리의 헤드라인 뉴스를 크롤링합니다.
     */
    Mono<ApiResponse<List<CategoryNewsResponseDto>>> crawlAllCategoryHeadlines();
}