package com.solra.crt.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpaceProject 实体单元测试。
 * 验证项目状态机、协作者管理、更新逻辑。
 */
@DisplayName("SpaceProject — 空间项目测试")
class SpaceProjectTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认状态为 DRAFT")
        void defaultStatusIsDraft() {
            SpaceProject project = createProject();
            assertEquals(SpaceProject.ProjectStatus.DRAFT, project.getStatus());
        }

        @Test
        @DisplayName("默认协作者列表为空")
        void defaultCollaboratorsEmpty() {
            SpaceProject project = createProject();
            assertTrue(project.getCollaboratorIds().isEmpty());
        }

        @Test
        @DisplayName("时间戳正确设置")
        void timestampsAreSet() {
            SpaceProject project = createProject();
            assertNotNull(project.getCreatedAt());
            assertNotNull(project.getUpdatedAt());
            assertNotNull(project.getLastEditedAt());
        }
    }

    @Nested
    @DisplayName("update() — 更新项目信息")
    class Update {

        @Test
        @DisplayName("同时更新 name 和 description")
        void updateBoth() {
            SpaceProject project = createProject();
            project.update("New Name", "New Desc");

            assertEquals("New Name", project.getName());
            assertEquals("New Desc", project.getDescription());
        }

        @Test
        @DisplayName("只更新 name，description 为 null 时保留原值")
        void updateNameOnly() {
            SpaceProject project = createProject();
            project.update("New Name", null);

            assertEquals("New Name", project.getName());
            assertNull(project.getDescription()); // original was null
        }

        @Test
        @DisplayName("只更新 description，name 为 null 时保留原值")
        void updateDescriptionOnly() {
            SpaceProject project = createProject();
            project.setDescription("Original Desc");
            project.update(null, "New Desc");

            assertEquals("My Space", project.getName()); // unchanged
            assertEquals("New Desc", project.getDescription());
        }

        @Test
        @DisplayName("全部 null 不改变任何值")
        void nullUpdateNoChange() {
            SpaceProject project = createProject();
            project.update(null, null);

            assertEquals("My Space", project.getName());
            assertNull(project.getDescription());
        }
    }

    @Nested
    @DisplayName("startBuilding() — 开始构建")
    class StartBuilding {

        @Test
        @DisplayName("状态变为 BUILDING")
        void becomesBuilding() {
            SpaceProject project = createProject();
            project.startBuilding();
            assertEquals(SpaceProject.ProjectStatus.BUILDING, project.getStatus());
        }
    }

    @Nested
    @DisplayName("publish() — 发布")
    class Publish {

        @Test
        @DisplayName("状态变为 PUBLISHED")
        void becomesPublished() {
            SpaceProject project = createProject();
            project.publish();
            assertEquals(SpaceProject.ProjectStatus.PUBLISHED, project.getStatus());
        }
    }

    @Nested
    @DisplayName("archive() — 归档")
    class Archive {

        @Test
        @DisplayName("状态变为 ARCHIVED")
        void becomesArchived() {
            SpaceProject project = createProject();
            project.archive();
            assertEquals(SpaceProject.ProjectStatus.ARCHIVED, project.getStatus());
        }
    }

    @Nested
    @DisplayName("addCollaborator() — 添加协作者")
    class AddCollaborator {

        @Test
        @DisplayName("添加新协作者")
        void addNewCollaborator() {
            SpaceProject project = createProject();
            project.addCollaborator("user-2");

            assertEquals(1, project.getCollaboratorIds().size());
            assertTrue(project.getCollaboratorIds().contains("user-2"));
        }

        @Test
        @DisplayName("添加重复协作者去重")
        void duplicateCollaboratorDeduplicated() {
            SpaceProject project = createProject();
            project.addCollaborator("user-2");
            project.addCollaborator("user-2");

            assertEquals(1, project.getCollaboratorIds().size());
        }

        @Test
        @DisplayName("添加多个协作者")
        void addMultipleCollaborators() {
            SpaceProject project = createProject();
            project.addCollaborator("user-2");
            project.addCollaborator("user-3");
            project.addCollaborator("user-4");

            assertEquals(3, project.getCollaboratorIds().size());
        }
    }

    @Nested
    @DisplayName("removeCollaborator() — 移除协作者")
    class RemoveCollaborator {

        @Test
        @DisplayName("移除存在的协作者")
        void removeExisting() {
            SpaceProject project = createProject();
            project.addCollaborator("user-2");
            project.addCollaborator("user-3");

            project.removeCollaborator("user-2");

            assertEquals(1, project.getCollaboratorIds().size());
            assertFalse(project.getCollaboratorIds().contains("user-2"));
            assertTrue(project.getCollaboratorIds().contains("user-3"));
        }

        @Test
        @DisplayName("移除不存在的协作者不报错")
        void removeNonExistingNoError() {
            SpaceProject project = createProject();
            project.removeCollaborator("non-existent");
            assertTrue(project.getCollaboratorIds().isEmpty());
        }
    }

    @Nested
    @DisplayName("updateConfig() — 更新配置")
    class UpdateConfig {

        @Test
        @DisplayName("设置项目配置")
        void setConfig() {
            SpaceProject project = createProject();
            ProjectConfig config = new ProjectConfig();
            project.updateConfig(config);

            assertNotNull(project.getConfig());
            assertEquals(config, project.getConfig());
        }
    }

    private SpaceProject createProject() {
        return new SpaceProject("proj-1", "space-1", "user-1",
                "My Space", SpaceProject.ProjectType.SPACE);
    }
}
