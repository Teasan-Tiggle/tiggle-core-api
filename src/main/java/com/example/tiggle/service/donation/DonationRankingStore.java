package com.example.tiggle.service.donation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DonationRankingStore {
    private final StringRedisTemplate redisTemplate;

    public void saveUniversityRanking(List<Object[]> results) {
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        String key = "donation:university";
        redisTemplate.delete(key);
        for (Object[] row : results) {
            String universityName = (String) row[0];
            Double total = ((BigDecimal) row[1]).doubleValue();
            zset.add(key, universityName, total);
        }
    }

    public Set<ZSetOperations.TypedTuple<String>> getUniversityRanking() {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores("donation:university", 0, 99); // Top 100
    }

    public void saveDepartmentRanking(Long universityId, List<Object[]> results) {
        ZSetOperations<String, String> zset = redisTemplate.opsForZSet();
        String key = "donation:department:" + universityId.toString();
        redisTemplate.delete(key);
        for (Object[] row : results) {
            String departmentName = (String) row[0];
            Double total = ((BigDecimal) row[1]).doubleValue();
            zset.add(key, departmentName, total);
        }
    }

    public Set<ZSetOperations.TypedTuple<String>> getDepartmentRanking(Long universityId) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores("donation:department:" + universityId.toString(), 0, 99);
    }
}
