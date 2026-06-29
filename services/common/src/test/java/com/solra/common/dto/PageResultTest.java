package com.solra.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResult 单元测试")
class PageResultTest {

    @Nested
    @DisplayName("of 工厂方法")
    class OfFactory {

        @Test
        @DisplayName("应正确构建分页结果")
        void shouldBuildPageResult() {
            List<String> items = List.of("a", "b", "c");
            PageResult<String> result = PageResult.of(items, 100, true, "cursor-xyz");
            assertEquals(items, result.items());
            assertEquals(100, result.total());
            assertTrue(result.hasMore());
            assertEquals("cursor-xyz", result.nextCursor());
        }

        @Test
        @DisplayName("hasMore=false 时应正确反映")
        void shouldReflectNoMorePages() {
            PageResult<String> result = PageResult.of(List.of("a"), 1, false, null);
            assertFalse(result.hasMore());
        }

        @Test
        @DisplayName("nextCursor 可为 null (最后一页)")
        void shouldAllowNullCursor() {
            PageResult<String> result = PageResult.of(List.of("a", "b"), 2, false, null);
            assertNull(result.nextCursor());
        }

        @Test
        @DisplayName("total 可以大于 items 数量 (游标分页)")
        void totalCanExceedItemsSize() {
            PageResult<String> result = PageResult.of(List.of("a"), 500, true, "next");
            assertEquals(1, result.items().size());
            assertEquals(500, result.total());
        }

        @Test
        @DisplayName("应支持 Integer 泛型")
        void shouldSupportIntegerType() {
            List<Integer> items = List.of(1, 2, 3);
            PageResult<Integer> result = PageResult.of(items, 3, false, null);
            assertEquals(3, result.items().size());
        }
    }

    @Nested
    @DisplayName("empty 工厂方法")
    class EmptyFactory {

        @Test
        @DisplayName("items 应为空列表")
        void shouldHaveEmptyItems() {
            PageResult<String> result = PageResult.empty();
            assertTrue(result.items().isEmpty());
        }

        @Test
        @DisplayName("total 应为 0")
        void shouldHaveZeroTotal() {
            PageResult<String> result = PageResult.empty();
            assertEquals(0, result.total());
        }

        @Test
        @DisplayName("hasMore 应为 false")
        void shouldHaveNoMore() {
            PageResult<String> result = PageResult.empty();
            assertFalse(result.hasMore());
        }

        @Test
        @DisplayName("nextCursor 应为 null")
        void shouldHaveNullCursor() {
            PageResult<String> result = PageResult.empty();
            assertNull(result.nextCursor());
        }

        @Test
        @DisplayName("多次调用 empty() 应返回等价结果")
        void shouldReturnEquivalentResults() {
            PageResult<String> r1 = PageResult.empty();
            PageResult<String> r2 = PageResult.empty();
            assertEquals(r1.items(), r2.items());
            assertEquals(r1.total(), r2.total());
            assertEquals(r1.hasMore(), r2.hasMore());
        }
    }

    @Nested
    @DisplayName("Record 特性")
    class RecordFeatures {

        @Test
        @DisplayName("相同字段值应 equals 相等")
        void shouldBeEqualWithSameFields() {
            PageResult<String> r1 = PageResult.of(List.of("a"), 1, false, null);
            PageResult<String> r2 = PageResult.of(List.of("a"), 1, false, null);
            assertEquals(r1, r2);
        }

        @Test
        @DisplayName("不同字段值应 equals 不等")
        void shouldNotBeEqualWithDifferentFields() {
            PageResult<String> r1 = PageResult.of(List.of("a"), 1, false, null);
            PageResult<String> r2 = PageResult.of(List.of("a"), 2, false, null);
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("toString 应包含字段值")
        void toStringShouldContainFieldValues() {
            PageResult<String> result = PageResult.of(List.of("x"), 42, true, "cursor-1");
            String str = result.toString();
            assertTrue(str.contains("42"));
            assertTrue(str.contains("true"));
            assertTrue(str.contains("cursor-1"));
        }

        @Test
        @DisplayName("hashCode 相同字段应相同")
        void hashCodeShouldBeConsistent() {
            PageResult<String> r1 = PageResult.of(List.of("a"), 1, false, null);
            PageResult<String> r2 = PageResult.of(List.of("a"), 1, false, null);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }
}
