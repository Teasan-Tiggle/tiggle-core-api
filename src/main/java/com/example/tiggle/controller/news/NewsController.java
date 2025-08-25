package com.example.tiggle.controller.news;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.news.CategoryNewsResponseDto;
import com.example.tiggle.service.news.NewsCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(name = "뉴스 API", description = "뉴스 크롤링 및 조회 관련 API")
public class NewsController {
    
    private final NewsCrawlerService newsCrawlerService;

    @Operation(summary = "전체 카테고리 헤드라인 뉴스 조회", description = "네이버 뉴스의 모든 카테고리별 헤드라인 뉴스를 조회합니다.")
    @GetMapping("/headlines")
    public ResponseEntity<ApiResponse<List<CategoryNewsResponseDto>>> getHeadlineNews() {
        try {
            List<CategoryNewsResponseDto> headlines = newsCrawlerService.crawlAllCategoryHeadlines();
            return ResponseEntity.ok(ApiResponse.success(headlines));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.failure("헤드라인 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
