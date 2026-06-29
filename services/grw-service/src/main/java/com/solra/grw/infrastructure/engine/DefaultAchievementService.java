package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.Achievement;
import com.solra.grw.domain.repository.AchievementRepository;
import com.solra.grw.domain.repository.ExperienceEventRepository;
import com.solra.grw.domain.repository.UserProfileRepository;
import com.solra.grw.domain.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultAchievementService — 成就系统实现。
 * GRW-005: ≥30个成就，每成就含专属徽章+动效+音效。
 */
@Component
public class DefaultAchievementService implements AchievementService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAchievementService.class);

    /** 事件类型到成就编码的映射 */
    private static final Map<String, List<String>> EVENT_TO_ACHIEVEMENT = Map.of(
        "SPACE_EXPLORE", List.of("FIRST_SPACE", "SPACE_EXPLORER_5", "SPACE_EXPLORER_20",
                "SPACE_EXPLORER_50", "SPACE_EXPLORER_100"),
        "CONVERSATION", List.of("FIRST_CONVERSATION", "CONVERSATION_MASTER_50", "CONVERSATION_MASTER_500"),
        "FRIEND_ADD", List.of("FIRST_FRIEND", "FRIEND_COLLECTOR_10", "FRIEND_COLLECTOR_50"),
        "SHARE_CREATE", List.of("FIRST_SHARE", "SHARE_VIRAL_10"),
        "SPACE_CREATED", List.of("FIRST_CREATION", "CREATOR_5", "CREATOR_20"),
        "GESTURE", List.of("FIRST_GESTURE", "GESTURE_ENTHUSIAST_100"),
        "RETURN_VISIT", List.of("FIRST_RETURN", "DAILY_VISITOR_7", "DAILY_VISITOR_30"),
        "TIME_SPENT", List.of("TIME_SPENT_1H", "TIME_SPENT_10H", "TIME_SPENT_100H"),
        "AVATAR_COLLECT", List.of("FIRST_AVATAR", "AVATAR_COLLECTOR_5", "AVATAR_COLLECTOR_10", "FULL_COLLECTION"),
        "LEVEL_UP", List.of("LEVEL_10", "LEVEL_30"),
        "EVANGELIST", List.of("EVANGELIST"),
        "ALL_ACHIEVEMENTS", List.of("ALL_ACHIEVEMENTS_20", "ALL_ACHIEVEMENTS_30")
    );

    /** 成就定义缓存 */
    private final Map<String, Achievement> definitionCache = new ConcurrentHashMap<>();
    /** 用户成就状态: userId -> {code -> unlocked} */
    private final Map<String, Map<String, Boolean>> userAchievements = new ConcurrentHashMap<>();

    private final AchievementRepository achievementRepo;
    private final UserProfileRepository userProfileRepo;
    private final ExperienceEventRepository experienceEventRepo;

    public DefaultAchievementService(AchievementRepository achievementRepo,
                                      UserProfileRepository userProfileRepo,
                                      ExperienceEventRepository experienceEventRepo) {
        this.achievementRepo = achievementRepo;
        this.userProfileRepo = userProfileRepo;
        this.experienceEventRepo = experienceEventRepo;
        ensureDefaultAchievements();
    }

    /** 确保预定义成就已入库 */
    private void ensureDefaultAchievements() {
        List<Achievement> all = Achievement.allAchievements();
        for (Achievement ach : all) {
            achievementRepo.findByCode(ach.getCode()).orElseGet(() -> {
                ach.setBadgeEffect(getDefaultEffect(ach.getRarity()));
                ach.setSoundEffect(getDefaultSound(ach.getRarity()));
                return achievementRepo.save(ach);
            });
            definitionCache.put(ach.getCode(), ach);
        }
        log.info("GRW-005 loaded {} achievement definitions", definitionCache.size());
    }

    private String getDefaultEffect(Achievement.Rarity rarity) {
        return switch (rarity) {
            case COMMON -> "sparkle";
            case UNCOMMON -> "glow";
            case RARE -> "confetti";
            case EPIC -> "rays";
            case LEGENDARY -> "rainbow_burst";
        };
    }

    private String getDefaultSound(Achievement.Rarity rarity) {
        return switch (rarity) {
            case COMMON, UNCOMMON -> "achievement_unlock";
            case RARE, EPIC -> "rare_fanfare";
            case LEGENDARY -> "legendary_anthem";
        };
    }

    @Override
    public List<AchievementUnlockResult> checkAndUnlock(String userId, String eventType, int currentProgress) {
        List<AchievementUnlockResult> results = new ArrayList<>();
        List<String> relatedCodes = EVENT_TO_ACHIEVEMENT.getOrDefault(eventType, List.of());

        // 确保用户状态存在
        userAchievements.putIfAbsent(userId, new ConcurrentHashMap<>());
        Map<String, Boolean> userStatus = userAchievements.get(userId);

        for (String code : relatedCodes) {
            Achievement ach = definitionCache.get(code);
            if (ach == null) continue;
            // 已解锁则跳过
            if (userStatus.getOrDefault(code, false)) continue;

            // 检查前置条件
            Set<String> unlockedCodes = new HashSet<>();
            userStatus.forEach((k, v) -> { if (v) unlockedCodes.add(k); });
            if (!ach.hasAllPrerequisites(unlockedCodes)) continue;

            // 检查进度
            if (ach.isUnlockable(currentProgress)) {
                userStatus.put(code, true);
                results.add(new AchievementUnlockResult(
                    ach.getAchievementId(), ach.getCode(), ach.getName(),
                    ach.getCategory(), ach.getRarity(),
                    ach.getBadgeEffect(), ach.getSoundEffect(),
                    ach.getExperienceReward(), true
                ));
                log.info("GRW-005 achievement unlocked: user={} code={} name={}",
                        userId, code, ach.getName());
            }
        }

        // 检查 ALL_ACHIEVEMENTS 成就
        long totalUnlocked = userStatus.values().stream().filter(v -> v).count();
        if (totalUnlocked >= 20 && !userStatus.getOrDefault("ALL_ACHIEVEMENTS_20", false)) {
            userStatus.put("ALL_ACHIEVEMENTS_20", true);
            Achievement ach = definitionCache.get("ALL_ACHIEVEMENTS_20");
            if (ach != null) {
                results.add(new AchievementUnlockResult(ach.getAchievementId(), ach.getCode(),
                    ach.getName(), ach.getCategory(), ach.getRarity(),
                    ach.getBadgeEffect(), ach.getSoundEffect(), ach.getExperienceReward(), true));
            }
        }
        if (totalUnlocked >= 30 && !userStatus.getOrDefault("ALL_ACHIEVEMENTS_30", false)) {
            userStatus.put("ALL_ACHIEVEMENTS_30", true);
            Achievement ach = definitionCache.get("ALL_ACHIEVEMENTS_30");
            if (ach != null) {
                results.add(new AchievementUnlockResult(ach.getAchievementId(), ach.getCode(),
                    ach.getName(), ach.getCategory(), ach.getRarity(),
                    ach.getBadgeEffect(), ach.getSoundEffect(), ach.getExperienceReward(), true));
            }
        }

        return results;
    }

    @Override
    public Map<String, Boolean> getUserAchievementStatus(String userId) {
        return userAchievements.getOrDefault(userId, Map.of());
    }

    @Override
    public List<Achievement> getUnlockedAchievements(String userId) {
        Map<String, Boolean> status = userAchievements.getOrDefault(userId, Map.of());
        return status.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> definitionCache.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<AchievementProgress> getLockedAchievements(String userId) {
        Map<String, Boolean> status = userAchievements.getOrDefault(userId, Map.of());
        List<AchievementProgress> progressList = new ArrayList<>();

        // 获取各维度的当前进度
        UserProfile profile = userProfileRepo.findByUserId(userId).orElse(null);
        int spacesVisited = profile != null ? profile.getSpacesVisited() : 0;
        int conversations = profile != null ? profile.getConversationsHad() : 0;
        int friends = profile != null ? profile.getFriendsCount() : 0;
        int interactions = profile != null ? profile.getTotalInteractions() : 0;

        for (Achievement ach : definitionCache.values()) {
            if (status.getOrDefault(ach.getCode(), false)) continue;
            int currentProgress = estimateProgress(ach.getCode(), spacesVisited, conversations,
                    friends, interactions);
            progressList.add(new AchievementProgress(
                ach.getAchievementId(), ach.getCode(), ach.getName(),
                ach.getCategory(), ach.getRarity(),
                ach.getRequiredCount(), currentProgress,
                ach.progressPercentage(currentProgress),
                ach.isUnlockable(currentProgress)
            ));
        }
        return progressList;
    }

    private int estimateProgress(String code, int spaces, int conversations, int friends, int interactions) {
        return switch (code) {
            case "FIRST_SPACE", "SPACE_EXPLORER_5", "SPACE_EXPLORER_20",
                 "SPACE_EXPLORER_50", "SPACE_EXPLORER_100" -> spaces;
            case "FIRST_CONVERSATION", "CONVERSATION_MASTER_50", "CONVERSATION_MASTER_500" -> conversations;
            case "FIRST_FRIEND", "FRIEND_COLLECTOR_10", "FRIEND_COLLECTOR_50" -> friends;
            default -> interactions;
        };
    }

    @Override
    public AchievementStats getGlobalStats() {
        int total = definitionCache.size();
        Map<Achievement.Rarity, Long> rarityDist = new LinkedHashMap<>();
        for (Achievement.Rarity r : Achievement.Rarity.values()) rarityDist.put(r, 0L);
        for (Achievement ach : definitionCache.values()) {
            rarityDist.merge(ach.getRarity(), 1L, Long::sum);
        }
        return new AchievementStats(total, total, rarityDist, Map.of());
    }

    @Override
    public AchievementCompletion getCompletion(String userId) {
        Map<String, Boolean> status = userAchievements.getOrDefault(userId, Map.of());
        int total = definitionCache.size();
        int unlocked = (int) status.values().stream().filter(v -> v).count();
        double percent = total > 0 ? (double) unlocked / total : 0.0;

        Map<Achievement.Category, CategoryCompletion> byCategory = new LinkedHashMap<>();
        for (Achievement.Category cat : Achievement.Category.values()) {
            int catTotal = (int) definitionCache.values().stream()
                    .filter(a -> a.getCategory() == cat).count();
            int catUnlocked = (int) definitionCache.values().stream()
                    .filter(a -> a.getCategory() == cat && status.getOrDefault(a.getCode(), false))
                    .count();
            byCategory.put(cat, new CategoryCompletion(catTotal, catUnlocked,
                    catTotal > 0 ? (double) catUnlocked / catTotal : 0.0));
        }

        return new AchievementCompletion(userId, total, unlocked, percent, byCategory);
    }
}
