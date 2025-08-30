package com.ssafy.tiggle.service.shortform;

import com.ssafy.tiggle.dto.common.ApiResponse;
import com.ssafy.tiggle.dto.shortform.VideoResponseDto;
import com.ssafy.tiggle.dto.shortform.news.CategoryNewsResponseDto;
import com.ssafy.tiggle.entity.EsgCategory;
import com.ssafy.tiggle.entity.Video;
import com.ssafy.tiggle.service.shortform.news.NewsCrawlerService;
import com.ssafy.tiggle.service.shortform.script.ScriptGenerationService;
import com.ssafy.tiggle.service.shortform.video.VideoGenerationService;
import com.ssafy.tiggle.service.shortform.video.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortFormPipelineService {
    
    private final NewsCrawlerService newsCrawlerService;
    private final ScriptGenerationService scriptGenerationService;
    private final VideoGenerationService videoGenerationService;
    private final VideoService videoService;
    private final Random random = new Random();
    
    public Mono<ApiResponse<VideoResponseDto>> generateShortFormVideoFromNews(String title, String body) {
        log.info("사용자 입력 뉴스로 숏폼 비디오 생성 시작 - title: {}", title);
        
        // 스크립트 생성
        return scriptGenerationService.generateShortFormVideoScript(title, body)
            .flatMap(scriptResponse -> {
                if (!scriptResponse.isResult()) {
                    return Mono.just(ApiResponse.<VideoResponseDto>failure("스크립트 생성에 실패했습니다: " + scriptResponse.getMessage()));
                }
                
                // 비디오 생성
                return videoGenerationService.generateFullVideoFromSections(scriptResponse.getData())
                    .map(videoResponse -> {
                        if (!videoResponse.isResult()) {
                            return ApiResponse.<VideoResponseDto>failure("비디오 생성에 실패했습니다: " + videoResponse.getMessage());
                        }
                        
                        // 비디오 저장 및 DB 저장
                        try {
                            Video savedVideo = videoService.createVideo(
                                title,
                                videoResponse.getData(),
                                null
                            );
                            
                            VideoResponseDto responseDto = VideoResponseDto.from(savedVideo);
                            log.info("사용자 입력 뉴스 숏폼 비디오 생성 완료 - ID: {}, 제목: {}", savedVideo.getId(), savedVideo.getTitle());
                            
                            return ApiResponse.success(responseDto);
                            
                        } catch (Exception e) {
                            log.error("비디오 저장 실패", e);
                            return ApiResponse.<VideoResponseDto>failure("비디오 저장에 실패했습니다: " + e.getMessage());
                        }
                    });
            })
            .onErrorResume(e -> {
                log.error("사용자 입력 뉴스 숏폼 비디오 생성 파이프라인 실패", e);
                return Mono.just(ApiResponse.<VideoResponseDto>failure("비디오 생성 중 오류가 발생했습니다: " + e.getMessage()));
            });
    }
    
    public Mono<ApiResponse<VideoResponseDto>> generateShortFormVideo() {
        return newsCrawlerService.crawlAllCategoryHeadlines()
            .flatMap(newsListResponse -> {
                if (!newsListResponse.isResult() || newsListResponse.getData().isEmpty()) {
                    return Mono.just(ApiResponse.<VideoResponseDto>failure("뉴스 데이터를 가져올 수 없습니다."));
                }
                
                // 1. 뉴스 중 랜덤하게 1개 선택
                CategoryNewsResponseDto selectedNews = selectRandomNews(newsListResponse.getData());
                if (selectedNews == null || selectedNews.getArticles().isEmpty()) {
                    return Mono.just(ApiResponse.<VideoResponseDto>failure("해당 카테고리의 뉴스를 찾을 수 없습니다."));
                }
                
                var randomArticle = selectedNews.getArticles().get(random.nextInt(selectedNews.getArticles().size()));
                log.info("선택된 뉴스: {} - {}", selectedNews.getCategory(), randomArticle.getTitle());
                
                // 2. 스크립트 생성
                return scriptGenerationService.generateShortFormVideoScript(randomArticle.getTitle(), randomArticle.getBody())
                    .flatMap(scriptResponse -> {
                        if (!scriptResponse.isResult()) {
                            return Mono.just(ApiResponse.<VideoResponseDto>failure("스크립트 생성에 실패했습니다: " + scriptResponse.getMessage()));
                        }
                        
                        // 3. 비디오 생성
                        return videoGenerationService.generateFullVideoFromSections(scriptResponse.getData())
                            .map(videoResponse -> {
                                if (!videoResponse.isResult()) {
                                    return ApiResponse.<VideoResponseDto>failure("비디오 생성에 실패했습니다: " + videoResponse.getMessage());
                                }
                                
                                // 4. 비디오 저장 및 DB 저장
                                try {
                                    Video savedVideo = videoService.createVideo(
                                        randomArticle.getTitle(),
                                        videoResponse.getData(),
                                        null
                                    );
                                    
                                    VideoResponseDto responseDto = VideoResponseDto.from(savedVideo);
                                    log.info("숏폼 비디오 생성 완료 - ID: {}, 제목: {}", savedVideo.getId(), savedVideo.getTitle());
                                    
                                    return ApiResponse.success(responseDto);
                                    
                                } catch (Exception e) {
                                    log.error("비디오 저장 실패", e);
                                    return ApiResponse.<VideoResponseDto>failure("비디오 저장에 실패했습니다: " + e.getMessage());
                                }
                            });
                    });
            })
            .onErrorResume(e -> {
                log.error("숏폼 비디오 생성 파이프라인 실패", e);
                return Mono.just(ApiResponse.<VideoResponseDto>failure("비디오 생성 중 오류가 발생했습니다: " + e.getMessage()));
            });
    }
    
    private CategoryNewsResponseDto selectRandomNews(List<CategoryNewsResponseDto> newsList) {
        return newsList.get(random.nextInt(newsList.size()));
    }
}