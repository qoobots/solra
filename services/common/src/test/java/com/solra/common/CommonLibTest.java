package com.solra.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommonLib 单元测试")
class CommonLibTest {

    @Test
    @DisplayName("VERSION 应非空且为 SNAPSHOT")
    void versionShouldBeSnapshot() {
        assertNotNull(CommonLib.VERSION);
        assertTrue(CommonLib.VERSION.contains("SNAPSHOT"));
    }

    @Test
    @DisplayName("VERSION 应以 0. 开头")
    void versionShouldStartWithZero() {
        assertTrue(CommonLib.VERSION.startsWith("0."));
    }

    @Test
    @DisplayName("构造函数应不可访问")
    void constructorShouldBePrivate() throws Exception {
        Constructor<CommonLib> constructor = CommonLib.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));
    }
}
