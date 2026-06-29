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
}
