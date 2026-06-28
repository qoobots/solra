package com.solra.grw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户成长服务 (Growth Service)。
 * 管理用户画像、信誉等级、成就系统、徽章与排行榜。
 */
@SpringBootApplication(scanBasePackages = {"com.solra.grw", "com.solra.common"})
public class GrwServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrwServiceApplication.class, args);
    }
}
