package com.solra.grw.interfaces.grpc;

import com.solra.grw.application.dto.*;
import com.solra.grw.application.service.GrwApplicationService;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * GrwGrpcService — 用户成长服务 gRPC 接口层。
 * GRW-002 决定性时刻 + GRW-006 新用户引导。
 */
@GrpcService
public class GrwGrpcService {

    private static final Logger log = LoggerFactory.getLogger(GrwGrpcService.class);

    private final GrwApplicationService appService;

    public GrwGrpcService(GrwApplicationService appService) {
        this.appService = appService;
    }

    /** 获取用户画像 */
    public UserProfileResultDTO getProfile(String userId) {
        return appService.getUserProfile(userId);
    }

    /** 获取信誉等级 */
    public FaithLevelResultDTO getFaithLevel(String userId) {
        return appService.getFaithLevel(userId);
    }

    /** 记录用户行为并检测决定性时刻 */
    public List<DecisiveMomentResultDTO> recordAction(String userId, String eventType, int value) {
        return appService.recordUserAction(userId, eventType, value);
    }

    /** 获取经验值 */
    public ExperienceResultDTO getExperience(String userId) {
        return appService.getExperience(userId);
    }

    /** 启动引导 */
    public OnboardingPathResultDTO startOnboarding(String userId) {
        return appService.startOnboarding(userId);
    }

    /** 推进引导步骤 */
    public OnboardingStepResultDTO advanceOnboardingStep(String userId) {
        return appService.advanceOnboardingStep(userId);
    }

    /** 跳过引导步骤 */
    public OnboardingStepResultDTO skipOnboardingStep(String userId) {
        return appService.skipOnboardingStep(userId);
    }

    /** 获取引导进度 */
    public OnboardingPathResultDTO getOnboardingProgress(String userId) {
        return appService.getOnboardingProgress(userId);
    }

    /** 完成引导 */
    public OnboardingPathResultDTO completeOnboarding(String userId) {
        return appService.completeOnboarding(userId);
    }

    // ── GRW-007: 用户召回推送 ──

    /** 评估流失风险 */
    public ChurnRiskResultDTO evaluateChurnRisk(String userId, String avatarName) {
        return appService.evaluateChurnRisk(userId, avatarName);
    }

    /** 生成召回任务 */
    public List<RecallTaskResultDTO> generateRecallTasks(String userId, String avatarName) {
        return appService.generateRecallTasks(userId, avatarName);
    }

    /** 处理召回回调 */
    public RecallTaskResultDTO handleRecallCallback(String taskId, String action) {
        return appService.handleRecallCallback(taskId, action);
    }

    /** 获取召回历史 */
    public List<RecallTaskResultDTO> getRecallHistory(String userId) {
        return appService.getRecallHistory(userId);
    }

    /** 获取召回策略列表 */
    public List<RecallStrategyResultDTO> getRecallStrategies() {
        return appService.getRecallStrategies();
    }

    /** 创建/更新召回策略 */
    public RecallStrategyResultDTO saveRecallStrategy(String strategyId, String name,
                                                       String riskLevel, int inactiveDaysMin,
                                                       int inactiveDaysMax,
                                                       String titleTemplate, String messageTemplate,
                                                       List<String> channels) {
        return appService.saveRecallStrategy(strategyId, name, riskLevel, inactiveDaysMin,
                inactiveDaysMax, titleTemplate, messageTemplate, channels);
    }

    /** 获取召回统计 */
    public RecallStatsDTO getRecallStats() {
        return appService.getRecallStats();
    }

    // ===== GRW-001 等级与存在值 =====

    /** 增加经验值 */
    public LevelUpResultDTO addExperience(String userId, int amount, String eventType) {
        return appService.addExperience(userId, amount, eventType);
    }

    /** 获取等级进度 */
    public LevelProgressDTO getLevelProgress(String userId) {
        return appService.getLevelProgress(userId);
    }

    /** 获取存在值 */
    public double getPresenceScore(String userId) {
        return appService.getPresenceScore(userId);
    }

    // ===== GRW-005 成就系统 =====

    /** 检查并解锁成就 */
    public List<AchievementUnlockDTO> checkAchievements(String userId, String eventType, int progress) {
        return appService.checkAchievements(userId, eventType, progress);
    }

    /** 获取成就状态 */
    public Map<String, Boolean> getAchievementStatus(String userId) {
        return appService.getAchievementStatus(userId);
    }

    /** 获取已解锁成就 */
    public List<AchievementUnlockDTO> getUnlockedAchievements(String userId) {
        return appService.getUnlockedAchievements(userId);
    }

    /** 获取未解锁成就进度 */
    public List<AchievementProgressDTO> getLockedAchievements(String userId) {
        return appService.getLockedAchievements(userId);
    }

    /** 获取成就完成度 */
    public AchievementCompletionDTO getAchievementCompletion(String userId) {
        return appService.getAchievementCompletion(userId);
    }

    // ===== GRW-003 布道者体系 =====

    /** 提交布道者申请 */
    public EvangelistDTO applyEvangelist(String userId, String displayName, String bio) {
        return appService.applyEvangelist(userId, displayName, bio);
    }

    /** 审批布道者 */
    public EvangelistDTO reviewEvangelist(String applicationId, boolean approved,
                                           String reviewerId, String comment) {
        return appService.reviewEvangelist(applicationId, approved, reviewerId, comment);
    }

    /** 获取布道者状态 */
    public EvangelistDTO getEvangelistStatus(String userId) {
        return appService.getEvangelistStatus(userId).orElse(null);
    }

    /** 获取活跃布道者列表 */
    public List<EvangelistDTO> getActiveEvangelists(int page, int size) {
        return appService.getActiveEvangelists(page, size);
    }

    /** 暂停布道者 */
    public void suspendEvangelist(String userId, String reason) {
        appService.suspendEvangelist(userId, reason);
    }

    /** 获取布道者统计 */
    public EvangelistStatsDTO getEvangelistStats() {
        return appService.getEvangelistStats();
    }

    // ===== GRW-004 虚拟人收集与养成 =====

    /** 收集虚拟人 */
    public AvatarEntryDTO collectAvatar(String userId, String avatarTypeId, String name,
                                         String rarity, String element) {
        return appService.collectAvatar(userId, avatarTypeId, name, rarity, element);
    }

    /** 虚拟人养成 */
    public AvatarEntryDTO addAvatarExperience(String userId, String avatarTypeId, int amount) {
        return appService.addAvatarExperience(userId, avatarTypeId, amount);
    }

    /** 增加好感度 */
    public AvatarEntryDTO addAvatarAffection(String userId, String avatarTypeId, int amount) {
        return appService.addAvatarAffection(userId, avatarTypeId, amount);
    }

    /** 设置最爱 */
    public void setFavoriteAvatar(String userId, String avatarTypeId) {
        appService.setFavoriteAvatar(userId, avatarTypeId);
    }

    /** 获取图鉴进度 */
    public CollectionProgressDTO getCollectionProgress(String userId) {
        return appService.getCollectionProgress(userId);
    }

    /** 获取虚拟人列表 */
    public List<AvatarEntryDTO> getUserAvatars(String userId) {
        return appService.getUserAvatars(userId);
    }

    // ===== GRW-008 信仰体系可视化 =====

    /** 生成信仰仪表盘 */
    public FaithDashboardDTO generateFaithDashboard(String userId) {
        return appService.generateFaithDashboard(userId);
    }

    /** 获取信仰仪表盘 */
    public FaithDashboardDTO getFaithDashboard(String userId) {
        return appService.getFaithDashboard(userId);
    }

    /** 刷新信仰仪表盘 */
    public FaithDashboardDTO refreshFaithDashboard(String userId) {
        return appService.refreshFaithDashboard(userId);
    }

    /** 获取全球信仰统计 */
    public GlobalFaithStatsDTO getGlobalFaithStats() {
        return appService.getGlobalFaithStats();
    }
}
