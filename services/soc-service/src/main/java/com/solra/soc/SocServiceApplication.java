package com.solra.soc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 社交在场服务 (Social Presence Service)。
 * 管理多人空间会话、虚拟人姿态同步与社交互动。
 */
@SpringBootApplication(scanBasePackages = {"com.solra.soc", "com.solra.common"})
public class SocServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocServiceApplication.class, args);
    }
}
