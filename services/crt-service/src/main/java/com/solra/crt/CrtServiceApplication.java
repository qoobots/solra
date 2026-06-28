package com.solra.crt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 空间创作服务 (Space Creation Service)。
 * 管理空间项目、3D资产、场景编辑、模板系统与项目构建发布。
 */
@SpringBootApplication(scanBasePackages = {"com.solra.crt", "com.solra.common"})
public class CrtServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrtServiceApplication.class, args);
    }
}
