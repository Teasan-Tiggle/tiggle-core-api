package com.example.tiggle.service.news;

import com.example.tiggle.dto.news.CategoryNewsResponseDto;
import com.example.tiggle.dto.news.NewsArticleDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NewsCrawlerServiceImpl implements NewsCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(NewsCrawlerServiceImpl.class);

    // 네이버 뉴스 카테고리 매핑
    private static final Map<Integer, String> CATEGORY_MAP = Map.of(
            100, "정치",
            101, "경제", 
            102, "사회",
            103, "생활/문화",
            104, "세계",
            105, "IT/과학"
    );

    @Override
    public List<CategoryNewsResponseDto> crawlAllCategoryHeadlines() throws Exception {
        List<CategoryNewsResponseDto> allCategoryNews = new ArrayList<>();
        
        for (Map.Entry<Integer, String> entry : CATEGORY_MAP.entrySet()) {
            int categoryCode = entry.getKey();
            String categoryName = entry.getValue();
            
            try {
                List<NewsArticleDto> articles = crawlHeadlinesWithContentByCategory(categoryCode);
                allCategoryNews.add(new CategoryNewsResponseDto(categoryName, articles));
                logger.info("카테고리 '{}' 헤드라인 뉴스 크롤링 완료: {}개", categoryName, articles.size());
            } catch (Exception e) {
                logger.error("카테고리 '{}' 크롤링 중 오류 발생", categoryName, e);
                // 오류가 발생한 카테고리는 빈 리스트로 추가
                allCategoryNews.add(new CategoryNewsResponseDto(categoryName, new ArrayList<>()));
            }
        }
        
        return allCategoryNews;
    }

    private List<String> parseHeadlineUrlsByCategory(int categoryCode) throws Exception {
        List<String> headlineUrls = new ArrayList<>();
        String url = "https://news.naver.com/section/" + categoryCode;

        logger.info("카테고리 페이지 요청 URL: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        Element headlineList = doc.selectFirst("ul.sa_list");
        if (headlineList == null) {
            logger.warn("헤드라인 뉴스 리스트 영역을 찾지 못함 - categoryCode: {}", categoryCode);
            return headlineUrls;
        }

        Elements newsItems = headlineList.select("li.sa_item._SECTION_HEADLINE");
        logger.info("헤드라인 뉴스 아이템 개수: {}", newsItems.size());

        for (Element item : newsItems) {
            Element link = item.selectFirst("a.sa_text_title._NLOG_IMPRESSION");
            if (link != null) {
                String href = link.attr("href");
                if (!href.isEmpty() && href.startsWith("https://")) {
                    headlineUrls.add(href);
                    logger.debug("수집한 헤드라인 뉴스 URL: {}", href);
                }
            }
        }
        return headlineUrls;
    }

    private NewsArticleDto crawlTitleAndBody(String url) throws Exception {
        logger.info("뉴스 기사 크롤링 시작 - URL: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .get();

        String title = doc.title();

        Element bodyElement = doc.selectFirst("#dic_area");
        String bodyText = "";
        if (bodyElement != null) {
            bodyText = bodyElement.text();
            logger.info("본문 추출 성공 - 길이: {}", bodyText.length());
        } else {
            logger.warn("본문 영역을 찾지 못함 - URL: {}", url);
        }

        return new NewsArticleDto(url, title, bodyText);
    }

    private List<NewsArticleDto> crawlHeadlinesWithContentByCategory(int categoryCode) throws Exception {
        List<NewsArticleDto> results = new ArrayList<>();

        List<String> headlineUrls = parseHeadlineUrlsByCategory(categoryCode);
        logger.info("총 헤드라인 뉴스 URL 수집 개수: {}", headlineUrls.size());

        for (String url : headlineUrls) {
            try {
                NewsArticleDto data = crawlTitleAndBody(url);
                results.add(data);
            } catch (Exception e) {
                logger.error("뉴스 크롤링 중 오류 발생 URL: {}", url, e);
            }
        }
        return results;
    }
}