package com.solra.not;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知消息服务 (Notification Service)。
 * 管理站内通知、推送消息(APNs/FCM)、收件箱与通知偏好。
 */
@SpringBootApplication(scanBasePackages = {"com.solra.not", "com.solra.common"})
public class NotServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotServiceApplication.class, args);
    }
}
