package com.solra.grw.domain.service;

import com.solra.grw.domain.model.DecisiveMoment;
import java.util.List;
import java.util.Map;

/**
 * DecisiveMomentDetector — 决定性时刻检测器领域服务接口。
 * 检测用户旅程中从临时访客到活跃用户的关键转化节点。
 */
public interface DecisiveMomentDetector {

    /**
     * 检测用户的决定性时刻。
     *
     * @param userId       用户ID
     * @param recentActions 最近的用户行为列表
     * @param currentState 当前用户状态快照
     * @return 检测到的决定性时刻列表
     */
    List<DecisiveMoment> detectMoments(String userId, List<String> recentActions,
                                       Map<String, Object> currentState);

    /** 判断某时刻是否应该被触发 */
    boolean shouldTrigger(DecisiveMoment moment);

    /** 计算转化价值 (0.0-1.0) */
    double calculateConversionValue(Map<String, Object> before, Map<String, Object> after);
}
