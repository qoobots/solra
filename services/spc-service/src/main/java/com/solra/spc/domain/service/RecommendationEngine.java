package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;

import java.util.List;

/**
 * RecommendationEngine — 推荐引擎抽象（SPC-002/SPC-009）。
 * 四信号融合：停留时长 + 互动（点赞/分享）+ AI 反馈 + 显式兴趣。
 * 具体实现由 core/ 推荐算法引擎提供。
 */
public interface RecommendationEngine {

    /** 个性化推荐 */
    List<Recommendation> recommend(String userId, int limit, List<SpaceCategory> categories);

    /** 热门推荐 */
    List<Recommendation> popular(int limit, List<SpaceCategory> categories);

    /** 最新推荐 */
    List<Recommendation> newest(int limit, List<SpaceCategory> categories);

    /** 趋势推荐 */
    List<Recommendation> trending(int limit, List<SpaceCategory> categories);

    /** 上报用户行为（用于更新推荐模型） */
    void reportAction(UserAction action);
}
