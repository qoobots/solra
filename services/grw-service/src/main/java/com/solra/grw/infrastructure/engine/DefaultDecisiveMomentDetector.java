package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.DecisiveMoment;
import com.solra.grw.domain.model.DecisiveMomentType;
import com.solra.grw.domain.service.DecisiveMomentDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DefaultDecisiveMomentDetector — 基于规则的决定性时刻检测器。
 * 通过用户行为计数和状态变化检测关键转化节点。
 */
@Component
public class DefaultDecisiveMomentDetector implements DecisiveMomentDetector {

    private static final Logger log = LoggerFactory.getLogger(DefaultDecisiveMomentDetector.class);

    /** 转化阈值 (行为次数达到即触发) */
    private static final Map<String, Integer> THRESHOLDS = Map.of(
            "CONVERSATION", 1,
            "SPACE_EXPLORE", 1,
            "FRIEND_ADD", 1,
            "SHARE_CREATE", 1,
            "RETURN_VISIT", 10
    );

    @Override
    public List<DecisiveMoment> detectMoments(String userId, List<String> recentActions,
                                               Map<String, Object> currentState) {
        List<DecisiveMoment> moments = new ArrayList<>();

        int interactions = getInt(currentState, "totalInteractions", 0);
        int conversations = getInt(currentState, "conversationsHad", 0);
        int spaces = getInt(currentState, "spacesVisited", 0);
        int friends = getInt(currentState, "friendsCount", 0);

        // 检测各类决定性时刻
        if (conversations >= 1 && momentNotYetDetected(recentActions, "FIRST_CONVERSATION")) {
            DecisiveMoment dm = new DecisiveMoment(UUID.randomUUID().toString(), userId,
                    DecisiveMomentType.FIRST_CONVERSATION);
            dm.setConversionValue(0.6);
            moments.add(dm);
            log.info("GRW-002 FirstConversation detected: user={}", userId);
        }

        if (spaces >= 1 && momentNotYetDetected(recentActions, "FIRST_SPACE_EXPLORED")) {
            DecisiveMoment dm = new DecisiveMoment(UUID.randomUUID().toString(), userId,
                    DecisiveMomentType.FIRST_SPACE_EXPLORED);
            dm.setConversionValue(0.5);
            moments.add(dm);
        }

        if (friends >= 1 && momentNotYetDetected(recentActions, "FIRST_FRIEND_ADDED")) {
            DecisiveMoment dm = new DecisiveMoment(UUID.randomUUID().toString(), userId,
                    DecisiveMomentType.FIRST_FRIEND_ADDED);
            dm.setConversionValue(0.7);
            moments.add(dm);
        }

        if (recentActions.contains("SHARE_CREATE") && momentNotYetDetected(recentActions, "FIRST_SHARE")) {
            DecisiveMoment dm = new DecisiveMoment(UUID.randomUUID().toString(), userId,
                    DecisiveMomentType.FIRST_SHARE);
            dm.setConversionValue(0.8);
            moments.add(dm);
        }

        return moments;
    }

    @Override
    public boolean shouldTrigger(DecisiveMoment moment) {
        return moment.getConversionValue() >= 0.5;
    }

    @Override
    public double calculateConversionValue(Map<String, Object> before, Map<String, Object> after) {
        int beforeInt = getInt(before, "totalInteractions", 0);
        int afterInt = getInt(after, "totalInteractions", 0);
        if (afterInt <= beforeInt) return 0.0;
        double delta = afterInt - beforeInt;
        return Math.min(1.0, delta / 5.0);
    }

    private boolean momentNotYetDetected(List<String> actions, String momentType) {
        return actions.contains(momentType) || actions.contains(actionsToMomentAction(momentType));
    }

    private String actionsToMomentAction(String momentType) {
        return switch (momentType) {
            case "FIRST_CONVERSATION" -> "CONVERSATION";
            case "FIRST_SPACE_EXPLORED" -> "SPACE_EXPLORE";
            case "FIRST_FRIEND_ADDED" -> "FRIEND_ADD";
            case "FIRST_SHARE" -> "SHARE_CREATE";
            default -> momentType;
        };
    }

    private int getInt(Map<String, Object> state, String key, int def) {
        Object val = state.get(key);
        if (val instanceof Number n) return n.intValue();
        return def;
    }
}
