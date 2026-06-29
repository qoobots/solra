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
}
