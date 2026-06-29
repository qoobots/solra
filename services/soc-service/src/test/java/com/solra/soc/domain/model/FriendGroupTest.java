package com.solra.soc.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * FriendGroup 领域模型单元测试。
 */
class FriendGroupTest {

    @Test
    void shouldCreateGroupWithCorrectFields() {
        FriendGroup group = new FriendGroup("g1", "u1", "亲密好友", 1);

        assertEquals("g1", group.getGroupId());
        assertEquals("u1", group.getUserId());
        assertEquals("亲密好友", group.getGroupName());
        assertEquals(1, group.getSortOrder());
        assertNotNull(group.getCreatedAt());
        assertNotNull(group.getUpdatedAt());
        assertTrue(group.getMemberUserIds().isEmpty());
    }

    @Test
    void shouldAddMemberToGroup() {
        FriendGroup group = new FriendGroup("g1", "u1", "Test", 1);
        group.addMember("friend1");
        group.addMember("friend2");

        assertEquals(2, group.getMemberUserIds().size());
        assertTrue(group.contains("friend1"));
        assertTrue(group.contains("friend2"));
    }

    @Test
    void shouldNotAddDuplicateMember() {
        FriendGroup group = new FriendGroup("g1", "u1", "Test", 1);
        group.addMember("friend1");
        group.addMember("friend1");

        assertEquals(1, group.getMemberUserIds().size());
    }

    @Test
    void shouldRemoveMemberFromGroup() {
        FriendGroup group = new FriendGroup("g1", "u1", "Test", 1);
        group.addMember("friend1");
        group.addMember("friend2");
        group.removeMember("friend1");

        assertEquals(1, group.getMemberUserIds().size());
        assertFalse(group.contains("friend1"));
        assertTrue(group.contains("friend2"));
    }

    @Test
    void shouldRenameGroup() {
        FriendGroup group = new FriendGroup("g1", "u1", "Old Name", 1);
        group.rename("New Name");

        assertEquals("New Name", group.getGroupName());
    }

    @Test
    void shouldReorderGroup() {
        FriendGroup group = new FriendGroup("g1", "u1", "Test", 1);
        group.reorder(5);

        assertEquals(5, group.getSortOrder());
    }
}
