package com.solra.common.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SolraHealthIndicator 单元测试")
class SolraHealthIndicatorTest {

    @Test
    @DisplayName("health() 应返回 UP 状态")
    void shouldReturnUpStatus() {
        SolraHealthIndicator indicator = new SolraHealthIndicator();
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
    }

    @Test
    @DisplayName("应包含 service 详情")
    void shouldContainServiceDetail() {
        SolraHealthIndicator indicator = new SolraHealthIndicator();
        Health health = indicator.health();
        assertEquals("solra-common", health.getDetails().get("service"));
    }

    @Test
    @DisplayName("应包含 version 详情")
    void shouldContainVersionDetail() {
        SolraHealthIndicator indicator = new SolraHealthIndicator();
        Health health = indicator.health();
        assertNotNull(health.getDetails().get("version"));
    }

    @Test
    @DisplayName("health() 不应返回 null")
    void shouldNotReturnNull() {
        SolraHealthIndicator indicator = new SolraHealthIndicator();
        assertNotNull(indicator.health());
    }
}
