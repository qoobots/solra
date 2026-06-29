package com.solra.spc.infrastructure.engine;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.service.RecommendationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * MockRecommendationEngine — 模拟推荐引擎。
 * SPC-002: 四信号融合推荐 / SPC-009: 个性化推荐流。
 * 在 core/ 推荐算法引擎就绪前提供基础功能。
 */
@Component
public class MockRecommendationEngine implements RecommendationEngine {

    private static final Logger log = LoggerFactory.getLogger(MockRecommendationEngine.class);
    private final Map<String, List<UserAction>> userHistory = new HashMap<>();

    // 模拟空间 ID 池
    private static final List<String> MOCK_SPACE_IDS = List.of(
            "spc-001", "spc-002", "spc-003", "spc-004", "spc-005",
            "spc-006", "spc-007", "spc-008", "spc-009", "spc-010"
    );

    private static final String[][] REASONS = {
        {"你可能喜欢", "基于你的浏览历史"},
        {"热门趋势", "很多人正在访问"},
        {"新鲜发布", "刚刚发布"},
        {"与你兴趣相似", "与你喜欢过的空间类似"}
    };

    private int counter = 0;

    @Override
    public List<Recommendation> recommend(String userId, int limit, List<SpaceCategory> categories) {
        List<Recommendation> results = new ArrayList<>();
        String[] pool = MOCK_SPACE_IDS.toArray(new String[0]);
        for (int i = 0; i < Math.min(limit, pool.length); i++) {
            results.add(createRec(pool[(counter + i) % pool.length], i));
        }
        counter++;
        log.debug("MockRecommendationEngine: recommended {} spaces for user={}", results.size(), userId);
        return results;
    }

    @Override
    public List<Recommendation> popular(int limit, List<SpaceCategory> categories) {
        return recommend("popular", limit, categories);
    }

    @Override
    public List<Recommendation> newest(int limit, List<SpaceCategory> categories) {
        return recommend("newest", limit, categories);
    }

    @Override
    public List<Recommendation> trending(int limit, List<SpaceCategory> categories) {
        return recommend("trending", limit, categories);
    }

    @Override
    public void reportAction(UserAction action) {
        userHistory.computeIfAbsent(action.getUserId(), k -> new ArrayList<>()).add(action);
        log.debug("MockRecommendationEngine: reported action {} for user={}", action.getActionType(), action.getUserId());
    }

    private Recommendation createRec(String spaceId, int idx) {
        Recommendation rec = new Recommendation();
        rec.setSpaceId(spaceId);
        rec.setScore(new RecommendScore(0.5f + idx * 0.05f, 0.6f, 0.7f));
        rec.setRecommendReasons(List.of(REASONS[idx % REASONS.length]));
        rec.setGeneratedAt(Instant.now());
        return rec;
    }
}
