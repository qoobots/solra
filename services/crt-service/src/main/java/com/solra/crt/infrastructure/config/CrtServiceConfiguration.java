package com.solra.crt.infrastructure.config;

import com.solra.crt.application.service.AssetApplicationService;
import com.solra.crt.application.service.ProjectApplicationService;
import com.solra.crt.application.service.PublishingApplicationService;
import com.solra.crt.application.service.TemplateApplicationService;
import com.solra.crt.domain.repository.AssetRepository;
import com.solra.crt.domain.repository.ProjectRepository;
import com.solra.crt.domain.repository.TemplateRepository;
import com.solra.crt.infrastructure.persistence.InMemoryAssetRepository;
import com.solra.crt.infrastructure.persistence.InMemoryProjectRepository;
import com.solra.crt.infrastructure.persistence.InMemoryTemplateRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CRT 服务 Spring 配置。
 * 注册领域仓储实现和应用服务 Bean。
 */
@Configuration
public class CrtServiceConfiguration {

    @Bean
    public ProjectRepository projectRepository() {
        return new InMemoryProjectRepository();
    }

    @Bean
    public AssetRepository assetRepository() {
        return new InMemoryAssetRepository();
    }

    @Bean
    public TemplateRepository templateRepository() {
        return new InMemoryTemplateRepository();
    }

    @Bean
    public ProjectApplicationService projectApplicationService(
            ProjectRepository projectRepository,
            TemplateRepository templateRepository) {
        return new ProjectApplicationService(projectRepository, templateRepository);
    }

    @Bean
    public AssetApplicationService assetApplicationService(
            AssetRepository assetRepository) {
        return new AssetApplicationService(assetRepository);
    }

    @Bean
    public TemplateApplicationService templateApplicationService(
            TemplateRepository templateRepository) {
        return new TemplateApplicationService(templateRepository);
    }

    @Bean
    public PublishingApplicationService publishingApplicationService(
            ProjectRepository projectRepository) {
        return new PublishingApplicationService(projectRepository);
    }
}
