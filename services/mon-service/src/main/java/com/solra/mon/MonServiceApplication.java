package com.solra.mon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.solra.mon", "com.solra.common"})
@EnableScheduling
public class MonServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonServiceApplication.class, args);
    }
}
