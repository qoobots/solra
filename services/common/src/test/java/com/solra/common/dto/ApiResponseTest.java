package com.solra.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiResponse 单元测试")
class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("success 工厂方法")
    class SuccessFactory {

        @Test
        @DisplayName("success(data) 应 code=0, message=success")
        void shouldHaveDefaultSuccessFields() {
            ApiResponse<String> resp = ApiResponse.success("hello");
            assertEquals(0, resp.code());
            assertEquals("success", resp.message());
            assertEquals("hello", resp.data());
        }

        @Test
        @DisplayName("success(data) traceId 应为 null")
        void shouldHaveNullTraceId() {
            ApiResponse<String> resp = ApiResponse.success("hello");
            assertNull(resp.traceId());
        }

        @Test
        @DisplayName("success(data) timestamp 应为当前时间附近")
        void shouldHaveRecentTimestamp() {
            Instant before = Instant.now();
            ApiResponse<String> resp = ApiResponse.success("hello");
            Instant after = Instant.now();
            assertFalse(resp.timestamp().isBefore(before.minusSeconds(1)));
            assertFalse(resp.timestamp().isAfter(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("success(data, traceId) 应设置 traceId")
        void shouldSetTraceId() {
            ApiResponse<String> resp = ApiResponse.success("hello", "trace-abc-123");
            assertEquals("trace-abc-123", resp.traceId());
            assertEquals(0, resp.code());
            assertEquals("success", resp.message());
        }

        @Test
        @DisplayName("success 支持 null data")
        void shouldSupportNullData() {
            ApiResponse<Void> resp = ApiResponse.success(null);
            assertEquals(0, resp.code());
            assertNull(resp.data());
        }
    }

    @Nested
    @DisplayName("error 工厂方法")
    class ErrorFactory {

        @Test
        @DisplayName("error(code, message) data 应为 null")
        void shouldHaveNullData() {
            ApiResponse<String> resp = ApiResponse.error(404, "Not Found");
            assertNull(resp.data());
        }

        @Test
        @DisplayName("error(code, message) traceId 应为 null")
        void shouldHaveNullTraceIdByDefault() {
            ApiResponse<String> resp = ApiResponse.error(500, "Error");
            assertNull(resp.traceId());
        }

        @Test
        @DisplayName("error(code, message, traceId) 应设置所有字段")
        void shouldSetAllFieldsWithTraceId() {
            ApiResponse<String> resp = ApiResponse.error(500, "Internal Error", "trace-xyz");
            assertEquals(500, resp.code());
            assertEquals("Internal Error", resp.message());
            assertNull(resp.data());
            assertEquals("trace-xyz", resp.traceId());
        }
    }

    @Nested
    @DisplayName("JSON 序列化")
    class JsonSerialization {

        @Test
        @DisplayName("成功响应应序列化 data 字段")
        void shouldSerializeDataField() throws JsonProcessingException {
            ApiResponse<String> resp = ApiResponse.success("hello");
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"data\":\"hello\""));
        }

        @Test
        @DisplayName("成功响应不应序列化 null traceId")
        void shouldExcludeNullTraceId() throws JsonProcessingException {
            ApiResponse<String> resp = ApiResponse.success("hello");
            String json = objectMapper.writeValueAsString(resp);
            assertFalse(json.contains("traceId"), "null traceId 不应出现在 JSON 中");
        }

        @Test
        @DisplayName("错误响应不应序列化 null data")
        void shouldExcludeNullData() throws JsonProcessingException {
            ApiResponse<String> resp = ApiResponse.error(404, "Not Found");
            String json = objectMapper.writeValueAsString(resp);
            assertFalse(json.contains("\"data\""), "null data 不应出现在 JSON 中");
        }

        @Test
        @DisplayName("带 traceId 的错误响应应包含 traceId")
        void shouldIncludeTraceIdWhenPresent() throws JsonProcessingException {
            ApiResponse<String> resp = ApiResponse.error(500, "Error", "trace-1");
            String json = objectMapper.writeValueAsString(resp);
            assertTrue(json.contains("\"traceId\":\"trace-1\""));
        }
    }

    @Nested
    @DisplayName("泛型支持")
    class GenericSupport {

        @Test
        @DisplayName("应支持 String 类型 data")
        void shouldSupportStringData() {
            ApiResponse<String> resp = ApiResponse.success("text");
            assertEquals("text", resp.data());
            assertEquals(String.class, resp.data().getClass());
        }

        @Test
        @DisplayName("应支持 Integer 类型 data")
        void shouldSupportIntegerData() {
            ApiResponse<Integer> resp = ApiResponse.success(42);
            assertEquals(42, resp.data());
        }

        @Test
        @DisplayName("应支持自定义对象类型 data")
        void shouldSupportCustomObjectData() {
            record TestObj(String name, int value) {}
            TestObj obj = new TestObj("test", 100);
            ApiResponse<TestObj> resp = ApiResponse.success(obj);
            assertEquals("test", resp.data().name());
            assertEquals(100, resp.data().value());
        }
    }
}
