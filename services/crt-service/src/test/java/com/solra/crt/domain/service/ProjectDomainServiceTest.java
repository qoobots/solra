package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.ProjectConfig;
import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ProjectDomainService 单元测试。
 * CRT-002 项目创建/构建/发布 + 模板分类映射。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectDomainService — 项目领域服务测试")
class ProjectDomainServiceTest {

    @Mock private ProjectRepository projectRepository;

    private ProjectDomainService service;

    @BeforeEach
    void setUp() {
        service = new ProjectDomainService(projectRepository);
    }

    @Nested
    @DisplayName("createProject — 创建项目")
    class CreateProject {

        @Test
        @DisplayName("正常创建 SPACE 类型项目")
        void createSpaceProject() {
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createProject("proj-1", "space-1", "user-1",
                    "My Space", SpaceProject.ProjectType.SPACE);

            assertEquals("proj-1", project.getProjectId());
            assertEquals("space-1", project.getSpaceId());
            assertEquals("user-1", project.getOwnerId());
            assertEquals("My Space", project.getName());
            assertEquals(SpaceProject.ProjectType.SPACE, project.getType());
            assertEquals(SpaceProject.ProjectStatus.DRAFT, project.getStatus());
            assertTrue(project.getCollaboratorIds().isEmpty());
        }

        @Test
        @DisplayName("创建 GALLERY 类型项目")
        void createGalleryProject() {
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createProject("proj-2", "space-1", "user-1",
                    "Gallery", SpaceProject.ProjectType.GALLERY);

            assertEquals(SpaceProject.ProjectType.GALLERY, project.getType());
        }

        @Test
        @DisplayName("创建 AVATAR 类型项目")
        void createAvatarProject() {
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createProject("proj-3", "space-1", "user-1",
                    "Avatar", SpaceProject.ProjectType.AVATAR);

            assertEquals(SpaceProject.ProjectType.AVATAR, project.getType());
        }

        @Test
        @DisplayName("创建 SCENE 类型项目")
        void createSceneProject() {
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createProject("proj-4", "space-1", "user-1",
                    "Scene", SpaceProject.ProjectType.SCENE);

            assertEquals(SpaceProject.ProjectType.SCENE, project.getType());
        }
    }

    @Nested
    @DisplayName("createFromTemplate — 从模板创建")
    class CreateFromTemplate {

        @Test
        @DisplayName("从 SPACE 模板创建 → SPACE 类型")
        void spaceTemplateMapsToSpace() {
            Template template = new Template("tpl-1", "Basic Space", "A basic space",
                    "author-1", Template.TemplateCategory.SPACE);
            template.setDefaultConfig(new ProjectConfig());

            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createFromTemplate("proj-1", "space-1",
                    "user-1", "From Template", template);

            assertEquals(SpaceProject.ProjectType.SPACE, project.getType());
        }

        @Test
        @DisplayName("从 GALLERY 模板创建 → GALLERY 类型")
        void galleryTemplateMapsToGallery() {
            Template template = new Template("tpl-2", "Gallery", "A gallery",
                    "author-1", Template.TemplateCategory.GALLERY);

            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createFromTemplate("proj-2", "space-1",
                    "user-1", "Gallery", template);

            assertEquals(SpaceProject.ProjectType.GALLERY, project.getType());
        }

        @Test
        @DisplayName("从 AVATAR 模板创建 → AVATAR 类型")
        void avatarTemplateMapsToAvatar() {
            Template template = new Template("tpl-3", "Avatar", "An avatar",
                    "author-1", Template.TemplateCategory.AVATAR);

            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createFromTemplate("proj-3", "space-1",
                    "user-1", "Avatar", template);

            assertEquals(SpaceProject.ProjectType.AVATAR, project.getType());
        }

        @Test
        @DisplayName("从 GAME 模板创建 → SCENE 类型")
        void gameTemplateMapsToScene() {
            Template template = new Template("tpl-4", "Game", "A game scene",
                    "author-1", Template.TemplateCategory.GAME);

            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createFromTemplate("proj-4", "space-1",
                    "user-1", "Game", template);

            assertEquals(SpaceProject.ProjectType.SCENE, project.getType());
        }

        @Test
        @DisplayName("从模板创建时继承默认配置")
        void inheritsDefaultConfig() {
            ProjectConfig config = new ProjectConfig();
            Template template = new Template("tpl-1", "Basic Space", "desc",
                    "author-1", Template.TemplateCategory.SPACE);
            template.setDefaultConfig(config);

            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject project = service.createFromTemplate("proj-1", "space-1",
                    "user-1", "From Template", template);

            assertNotNull(project.getConfig());
        }
    }

    @Nested
    @DisplayName("buildProject — 构建项目")
    class BuildProject {

        @Test
        @DisplayName("正常发起构建")
        void buildSuccess() {
            SpaceProject project = new SpaceProject("proj-1", "space-1", "user-1",
                    "My Space", SpaceProject.ProjectType.SPACE);

            when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject built = service.buildProject("proj-1");

            assertEquals(SpaceProject.ProjectStatus.BUILDING, built.getStatus());
        }

        @Test
        @DisplayName("项目不存在时抛异常")
        void projectNotFoundThrows() {
            when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.buildProject("non-existent"));
        }
    }

    @Nested
    @DisplayName("publishProject — 发布项目")
    class PublishProject {

        @Test
        @DisplayName("正常发布项目")
        void publishSuccess() {
            SpaceProject project = new SpaceProject("proj-1", "space-1", "user-1",
                    "My Space", SpaceProject.ProjectType.SPACE);

            when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
            when(projectRepository.save(any(SpaceProject.class))).thenAnswer(inv -> inv.getArgument(0));

            SpaceProject published = service.publishProject("proj-1");

            assertEquals(SpaceProject.ProjectStatus.PUBLISHED, published.getStatus());
        }

        @Test
        @DisplayName("项目不存在时抛异常")
        void projectNotFoundThrows() {
            when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.publishProject("non-existent"));
        }
    }
}
