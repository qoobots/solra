package com.solra.soc.infrastructure.config;

import com.solra.soc.application.service.SocApplicationService;
import com.solra.soc.domain.repository.FriendRepository;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.solra.soc.domain.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SOC 服务 Spring 配置。
 * 注册领域服务和应用服务 Bean。
 */
@Configuration
public class SocServiceConfiguration {

    // ── 领域服务 ──

    @Bean
    public SessionManager sessionManager() {
        return new SessionManager();
    }

    @Bean
    public ChatService chatService() {
        return new ChatService();
    }

    @Bean
    public SocialGestureService socialGestureService() {
        return new SocialGestureService();
    }

    @Bean
    public SpatialAudioEngine spatialAudioEngine() {
        return new SpatialAudioEngine();
    }

    // ── 应用服务 ──

    @Bean
    public SocApplicationService socApplicationService(
            SessionManager sessionManager,
            ChatService chatService,
            SocialGestureService gestureService,
            SpatialAudioEngine audioEngine,
            ShareEngine shareEngine,
            ShareSessionRepository shareSessionRepository,
            FriendRepository friendRepository) {
        return new SocApplicationService(sessionManager, chatService, gestureService,
                audioEngine, shareEngine, shareSessionRepository, friendRepository);
    }
}
