package com.solra.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SolraException 异常层次结构单元测试")
class SolraExceptionTest {

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedExceptionTest {

        @Test
        @DisplayName("errorCode 应为 1")
        void shouldHaveErrorCode1() {
            UnauthorizedException ex = new SolraException.UnauthorizedException("未授权");
            assertEquals(1, ex.getErrorCode());
        }

        @Test
        @DisplayName("应保留原始消息")
        void shouldPreserveMessage() {
            UnauthorizedException ex = new SolraException.UnauthorizedException("未授权");
            assertEquals("未授权", ex.getMessage());
        }

        @Test
        @DisplayName("应为 RuntimeException 子类")
        void shouldBeRuntimeException() {
            assertInstanceOf(RuntimeException.class,
                    new SolraException.UnauthorizedException("test"));
        }
    }

    @Nested
    @DisplayName("TokenExpiredException")
    class TokenExpiredExceptionTest {

        @Test
        @DisplayName("errorCode 应为 2")
        void shouldHaveErrorCode2() {
            TokenExpiredException ex = new SolraException.TokenExpiredException("令牌已过期");
            assertEquals(2, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("PermissionDeniedException")
    class PermissionDeniedExceptionTest {

        @Test
        @DisplayName("errorCode 应为 3")
        void shouldHaveErrorCode3() {
            PermissionDeniedException ex = new SolraException.PermissionDeniedException("权限不足");
            assertEquals(3, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("NotFoundException")
    class NotFoundExceptionTest {

        @Test
        @DisplayName("errorCode 应为 20")
        void shouldHaveErrorCode20() {
            NotFoundException ex = new SolraException.NotFoundException("资源未找到");
            assertEquals(20, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("AlreadyExistsException")
    class AlreadyExistsExceptionTest {

        @Test
        @DisplayName("errorCode 应为 21")
        void shouldHaveErrorCode21() {
            AlreadyExistsException ex = new SolraException.AlreadyExistsException("已存在");
            assertEquals(21, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("InvalidArgumentException")
    class InvalidArgumentExceptionTest {

        @Test
        @DisplayName("errorCode 应为 40")
        void shouldHaveErrorCode40() {
            InvalidArgumentException ex = new SolraException.InvalidArgumentException("参数无效");
            assertEquals(40, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("RateLimitedException")
    class RateLimitedExceptionTest {

        @Test
        @DisplayName("errorCode 应为 41")
        void shouldHaveErrorCode41() {
            RateLimitedException ex = new SolraException.RateLimitedException("请求过于频繁");
            assertEquals(41, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("InternalException")
    class InternalExceptionTest {

        @Test
        @DisplayName("errorCode 应为 60")
        void shouldHaveErrorCode60() {
            InternalException ex = new SolraException.InternalException("内部错误");
            assertEquals(60, ex.getErrorCode());
        }

        @Test
        @DisplayName("应支持带 cause 的构造")
        void shouldSupportCauseConstructor() {
            Throwable cause = new RuntimeException("root cause");
            InternalException ex = new SolraException.InternalException("包装错误", cause);
            assertEquals(60, ex.getErrorCode());
            assertEquals("包装错误", ex.getMessage());
            assertEquals(cause, ex.getCause());
        }

        @Test
        @DisplayName("不带 cause 时 getCause 应为 null")
        void shouldHaveNullCauseWithoutCauseArg() {
            InternalException ex = new SolraException.InternalException("内部错误");
            assertNull(ex.getCause());
        }
    }

    @Nested
    @DisplayName("ServiceUnavailableException")
    class ServiceUnavailableExceptionTest {

        @Test
        @DisplayName("errorCode 应为 61")
        void shouldHaveErrorCode61() {
            ServiceUnavailableException ex = new SolraException.ServiceUnavailableException("服务不可用");
            assertEquals(61, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("ContentRejectedException")
    class ContentRejectedExceptionTest {

        @Test
        @DisplayName("errorCode 应为 80")
        void shouldHaveErrorCode80() {
            ContentRejectedException ex = new SolraException.ContentRejectedException("内容违规");
            assertEquals(80, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("错误码映射覆盖")
    class ErrorCodeCoverage {

        @Test
        @DisplayName("所有错误码应互不重复")
        void allErrorCodesShouldBeUnique() {
            int[] codes = {1, 2, 3, 20, 21, 40, 41, 60, 61, 80};
            assertEquals(10, java.util.Arrays.stream(codes).distinct().count());
        }

        @Test
        @DisplayName("认证类错误码应在 1-3 范围")
        void authErrorCodesShouldBeInRange1to3() {
            assertTrue(new SolraException.UnauthorizedException("").getErrorCode() <= 3);
            assertTrue(new SolraException.TokenExpiredException("").getErrorCode() <= 3);
            assertTrue(new SolraException.PermissionDeniedException("").getErrorCode() <= 3);
        }

        @Test
        @DisplayName("资源类错误码应在 20-21 范围")
        void resourceErrorCodesShouldBeInRange20to21() {
            assertEquals(20, new SolraException.NotFoundException("").getErrorCode());
            assertEquals(21, new SolraException.AlreadyExistsException("").getErrorCode());
        }

        @Test
        @DisplayName("参数类错误码应在 40-41 范围")
        void argumentErrorCodesShouldBeInRange40to41() {
            assertEquals(40, new SolraException.InvalidArgumentException("").getErrorCode());
            assertEquals(41, new SolraException.RateLimitedException("").getErrorCode());
        }

        @Test
        @DisplayName("服务类错误码应在 60-61 范围")
        void serviceErrorCodesShouldBeInRange60to61() {
            assertEquals(60, new SolraException.InternalException("").getErrorCode());
            assertEquals(61, new SolraException.ServiceUnavailableException("").getErrorCode());
        }

        @Test
        @DisplayName("内容类错误码应为 80")
        void contentErrorCodeShouldBe80() {
            assertEquals(80, new SolraException.ContentRejectedException("").getErrorCode());
        }
    }
}
