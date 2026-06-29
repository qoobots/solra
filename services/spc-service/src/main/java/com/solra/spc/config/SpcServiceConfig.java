package com.solra.spc.config;

import com.solra.spc.domain.repository.SpaceRepository;
import com.solra.spc.domain.repository.UserActionRepository;
import com.solra.spc.domain.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SpcServiceConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public LeaderboardService leaderboardService(SpaceRepository spaceRepository) {
        return new LeaderboardService(spaceRepository);
    }

    @Bean
    public SpaceDomainService spaceDomainService(SpaceRepository spaceRepo, UserActionRepository actionRepo,
                                                   StreamingLoader streamingLoader, RecommendationEngine recommendationEngine,
                                                   SpaceSearchService searchService, PreloadManager preloadManager,
                                                   TransitionService transitionService, CdnDistributionService cdnService,
                                                   LeaderboardService leaderboardService) {
        return new SpaceDomainService(spaceRepo, actionRepo, streamingLoader, recommendationEngine,
                searchService, preloadManager, transitionService, cdnService, leaderboardService);
    }
}
