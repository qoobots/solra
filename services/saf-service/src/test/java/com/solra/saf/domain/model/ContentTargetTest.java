package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentTarget 值对象 单元测试")
class ContentTargetTest {

    @Nested
    @DisplayName("text — 文本内容工厂")
    class TextTests {

        @Test
        @DisplayName("创建文本目标 → 类型为 TEXT，计算 SHA-256 哈希")
        void createsTextTarget() {
            ContentTarget t = ContentTarget.text("id1", "hello world");

            assertEquals("id1", t.getContentId());
            assertEquals(ContentType.TEXT, t.getContentType());
            assertEquals("hello world", t.getContentText());
            assertNotNull(t.getContentHash());
            assertFalse(t.getContentHash().isEmpty());
        }

        @Test
        @DisplayName("相同内容产生相同哈希")
        void sameContentSameHash() {
            ContentTarget t1 = ContentTarget.text("a", "same");
            ContentTarget t2 = ContentTarget.text("b", "same");

            assertEquals(t1.getContentHash(), t2.getContentHash());
        }

        @Test
        @DisplayName("不同内容产生不同哈希")
        void differentContentDifferentHash() {
            ContentTarget t1 = ContentTarget.text("a", "hello");
            ContentTarget t2 = ContentTarget.text("b", "world");

            assertNotEquals(t1.getContentHash(), t2.getContentHash());
        }
    }

    @Nested
    @DisplayName("avatarSpeech — 虚拟人语音工厂")
    class AvatarSpeechTests {

        @Test
        @DisplayName("创建语音目标 → 类型为 AVATAR_SPEECH")
        void createsAvatarSpeechTarget() {
            ContentTarget t = ContentTarget.avatarSpeech("av1", "hello, i am your avatar");

            assertEquals("av1", t.getContentId());
            assertEquals(ContentType.AVATAR_SPEECH, t.getContentType());
            assertEquals("hello, i am your avatar", t.getContentText());
            assertNotNull(t.getContentHash());
        }
    }

    @Nested
    @DisplayName("spaceDescription — 空间描述工厂")
    class SpaceDescriptionTests {

        @Test
        @DisplayName("创建空间描述目标 → 类型为 SPACE_DESCRIPTION")
        void createsSpaceDescriptionTarget() {
            ContentTarget t = ContentTarget.spaceDescription("sp1", "a beautiful virtual space");

            assertEquals("sp1", t.getContentId());
            assertEquals(ContentType.SPACE_DESCRIPTION, t.getContentType());
            assertEquals("a beautiful virtual space", t.getContentText());
        }
    }

    @Nested
    @DisplayName("哈希边界情况")
    class HashEdgeCases {

        @Test
        @DisplayName("null 内容哈希为空字符串")
        void nullContentHashEmpty() {
            ContentTarget t = ContentTarget.text("id1", null);

            assertEquals("", t.getContentHash());
        }
    }
}
