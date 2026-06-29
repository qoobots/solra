package com.solra.crt.interfaces.grpc;

import com.solra.crt.application.dto.*;
import com.solra.crt.application.service.AssetApplicationService;
import com.solra.crt.application.service.ProjectApplicationService;
import com.solra.crt.application.service.PublishingApplicationService;
import com.solra.crt.application.service.TemplateApplicationService;
import com.solra.crt.domain.entity.ProjectConfig;
import com.solra.crt.domain.service.PublishingPipeline;
import com.solra.proto.crt.Crt;
import com.solra.proto.crt.SpaceCreationServiceGrpc;
import com.solra.proto.common.Common;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRT gRPC 服务实现。
 * 对应 proto SpaceCreationService，覆盖全部 14 个 RPC 方法。
 * 涵盖 CRT-001 (AI辅助搭建)、CRT-002 (可视化编辑器)、CRT-005 (资产库管理)、CRT-008 (发布审核)。
 */
@GrpcService
public class CrtGrpcService extends SpaceCreationServiceGrpc.SpaceCreationServiceImplBase {

    private final ProjectApplicationService projectService;
    private final AssetApplicationService assetService;
    private final TemplateApplicationService templateService;
    private final PublishingApplicationService publishingService;

    public CrtGrpcService(ProjectApplicationService projectService,
                          AssetApplicationService assetService,
                          TemplateApplicationService templateService,
                          PublishingApplicationService publishingService) {
        this.projectService = projectService;
        this.assetService = assetService;
        this.templateService = templateService;
        this.publishingService = publishingService;
    }

    // ═══════════════════════════════════════════════════════════
    // 项目管理 (CRT-001, CRT-002)
    // ═══════════════════════════════════════════════════════════

    @Override
    public void createProject(Crt.CreateProjectRequest request,
                              StreamObserver<Crt.CreateProjectResponse> responseObserver) {
        try {
            String spaceId = request.getSpaceId().getValue();
            String name = request.getName();
            String type = request.getType().name().replace("PROJECT_TYPE_", "");
            String templateId = request.getTemplateId().isEmpty() ? null : request.getTemplateId();

            ProjectDTO project = projectService.createProject(spaceId, extractUserId(), name, type, templateId);

            responseObserver.onNext(Crt.CreateProjectResponse.newBuilder()
                    .setProject(toProtoProject(project))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.CreateProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getProject(Crt.GetProjectRequest request,
                           StreamObserver<Crt.GetProjectResponse> responseObserver) {
        try {
            ProjectDTO project = projectService.getProject(request.getProjectId());
            responseObserver.onNext(Crt.GetProjectResponse.newBuilder()
                    .setProject(toProtoProject(project))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.GetProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(404)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void updateProject(Crt.UpdateProjectRequest request,
                              StreamObserver<Crt.UpdateProjectResponse> responseObserver) {
        try {
            ProjectDTO project = projectService.updateProject(
                    request.getProjectId(), request.getName(), request.getDescription());
            responseObserver.onNext(Crt.UpdateProjectResponse.newBuilder()
                    .setProject(toProtoProject(project))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.UpdateProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void deleteProject(Crt.DeleteProjectRequest request,
                              StreamObserver<Crt.DeleteProjectResponse> responseObserver) {
        try {
            projectService.deleteProject(request.getProjectId());
            responseObserver.onNext(Crt.DeleteProjectResponse.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.DeleteProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void listProjects(Crt.ListProjectsRequest request,
                             StreamObserver<Crt.ListProjectsResponse> responseObserver) {
        try {
            String ownerId = request.getOwnerId().getValue();
            String status = request.getStatusFilter().name().equals("PROJECT_STATUS_UNSPECIFIED") ?
                    null : request.getStatusFilter().name().replace("PROJECT_STATUS_", "");
            int page = request.getPage().getPage();
            int pageSize = request.getPage().getPageSize();

            PageResult<ProjectDTO> result = projectService.listProjects(ownerId, status, page, pageSize);

            responseObserver.onNext(Crt.ListProjectsResponse.newBuilder()
                    .addAllProjects(result.getItems().stream()
                            .map(this::toProtoProject)
                            .collect(Collectors.toList()))
                    .setPage(Common.PageResponse.newBuilder()
                            .setPage(page)
                            .setPageSize(pageSize)
                            .setTotal(result.getTotal())
                            .setTotalPages(result.getTotalPages())
                            .build())
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.ListProjectsResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ═══════════════════════════════════════════════════════════
    // 资产 CRUD (CRT-005)
    // ═══════════════════════════════════════════════════════════

    @Override
    public void uploadAsset(Crt.UploadAssetRequest request,
                            StreamObserver<Crt.UploadAssetResponse> responseObserver) {
        try {
            String spaceId = request.getSpaceId().getValue();
            String name = request.getName();
            String type = request.getType().name().replace("ASSET_TYPE_", "");

            AssetDTO asset = assetService.uploadAsset(spaceId, extractUserId(), name, type, "");

            responseObserver.onNext(Crt.UploadAssetResponse.newBuilder()
                    .setAsset(toProtoAsset(asset))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.UploadAssetResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getAsset(Crt.GetAssetRequest request,
                         StreamObserver<Crt.GetAssetResponse> responseObserver) {
        try {
            AssetDTO asset = assetService.getAsset(request.getAssetId());
            responseObserver.onNext(Crt.GetAssetResponse.newBuilder()
                    .setAsset(toProtoAsset(asset))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.GetAssetResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(404)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void listAssets(Crt.ListAssetsRequest request,
                           StreamObserver<Crt.ListAssetsResponse> responseObserver) {
        try {
            String spaceId = request.getSpaceId().getValue();
            String typeFilter = request.getTypeFilter().name().equals("ASSET_TYPE_UNSPECIFIED") ?
                    null : request.getTypeFilter().name().replace("ASSET_TYPE_", "");
            int page = request.getPage().getPage();
            int pageSize = request.getPage().getPageSize();

            PageResult<AssetDTO> result = assetService.listAssets(spaceId, typeFilter, page, pageSize);

            responseObserver.onNext(Crt.ListAssetsResponse.newBuilder()
                    .addAllAssets(result.getItems().stream()
                            .map(this::toProtoAsset)
                            .collect(Collectors.toList()))
                    .setPage(Common.PageResponse.newBuilder()
                            .setPage(page)
                            .setPageSize(pageSize)
                            .setTotal(result.getTotal())
                            .setTotalPages(result.getTotalPages())
                            .build())
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.ListAssetsResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void deleteAsset(Crt.DeleteAssetRequest request,
                            StreamObserver<Crt.DeleteAssetResponse> responseObserver) {
        try {
            assetService.deleteAsset(request.getAssetId());
            responseObserver.onNext(Crt.DeleteAssetResponse.newBuilder().build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.DeleteAssetResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ═══════════════════════════════════════════════════════════
    // 场景编辑 (CRT-002)
    // ═══════════════════════════════════════════════════════════

    @Override
    public void updateSceneGraph(Crt.UpdateSceneGraphRequest request,
                                  StreamObserver<Crt.UpdateSceneGraphResponse> responseObserver) {
        try {
            ProjectConfig.SceneGraph sceneGraph = fromProtoSceneGraph(request.getSceneGraph());
            ProjectDTO project = projectService.updateSceneGraph(request.getProjectId(), sceneGraph);
            responseObserver.onNext(Crt.UpdateSceneGraphResponse.newBuilder()
                    .setSceneGraph(request.getSceneGraph())
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.UpdateSceneGraphResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getSceneGraph(Crt.GetSceneGraphRequest request,
                               StreamObserver<Crt.GetSceneGraphResponse> responseObserver) {
        try {
            ProjectConfig.SceneGraph sceneGraph = projectService.getSceneGraph(request.getProjectId());
            responseObserver.onNext(Crt.GetSceneGraphResponse.newBuilder()
                    .setSceneGraph(toProtoSceneGraph(sceneGraph))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.GetSceneGraphResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(404)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ═══════════════════════════════════════════════════════════
    // 模板系统
    // ═══════════════════════════════════════════════════════════

    @Override
    public void listTemplates(Crt.ListTemplatesRequest request,
                               StreamObserver<Crt.ListTemplatesResponse> responseObserver) {
        try {
            String category = request.getCategory().name().equals("TEMPLATE_CATEGORY_UNSPECIFIED") ?
                    null : request.getCategory().name().replace("TEMPLATE_CATEGORY_", "");
            String keyword = request.getKeyword().isEmpty() ? null : request.getKeyword();
            int page = request.getPage().getPage();
            int pageSize = request.getPage().getPageSize();

            PageResult<TemplateDTO> result = templateService.listTemplates(category, keyword, page, pageSize);

            responseObserver.onNext(Crt.ListTemplatesResponse.newBuilder()
                    .addAllTemplates(result.getItems().stream()
                            .map(this::toProtoTemplate)
                            .collect(Collectors.toList()))
                    .setPage(Common.PageResponse.newBuilder()
                            .setPage(page)
                            .setPageSize(pageSize)
                            .setTotal(result.getTotal())
                            .setTotalPages(result.getTotalPages())
                            .build())
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.ListTemplatesResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void useTemplate(Crt.UseTemplateRequest request,
                            StreamObserver<Crt.UseTemplateResponse> responseObserver) {
        try {
            String spaceId = request.getSpaceId().getValue();
            String projectName = request.getProjectName();
            String templateId = request.getTemplateId();

            ProjectDTO project = projectService.createProject(spaceId, extractUserId(),
                    projectName, "SPACE", templateId);

            responseObserver.onNext(Crt.UseTemplateResponse.newBuilder()
                    .setProject(toProtoProject(project))
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.UseTemplateResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ═══════════════════════════════════════════════════════════
    // 构建与发布 (CRT-008)
    // ═══════════════════════════════════════════════════════════

    @Override
    public void buildProject(Crt.BuildProjectRequest request,
                             StreamObserver<Crt.BuildProjectResponse> responseObserver) {
        try {
            ProjectDTO project = projectService.buildProject(request.getProjectId());
            responseObserver.onNext(Crt.BuildProjectResponse.newBuilder()
                    .setBuildId("build-" + request.getProjectId())
                    .setProgress(1.0f)
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.BuildProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void publishProject(Crt.PublishProjectRequest request,
                               StreamObserver<Crt.PublishProjectResponse> responseObserver) {
        try {
            ProjectDTO project = projectService.publishProject(request.getProjectId());
            responseObserver.onNext(Crt.PublishProjectResponse.newBuilder()
                    .setProject(toProtoProject(project))
                    .setPublicUrl("solra://spaces/" + project.getSpaceId())
                    .build());
        } catch (Exception e) {
            responseObserver.onNext(Crt.PublishProjectResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder()
                            .setCode(400)
                            .setMessage(e.getMessage())
                            .build())
                    .build());
        }
        responseObserver.onCompleted();
    }

    // ═══════════════════════════════════════════════════════════
    // Proto 转换辅助方法
    // ═══════════════════════════════════════════════════════════

    private Crt.SpaceProject toProtoProject(ProjectDTO dto) {
        Crt.SpaceProject.Builder builder = Crt.SpaceProject.newBuilder()
                .setProjectId(dto.getProjectId())
                .setSpaceId(Common.SpaceId.newBuilder().setValue(dto.getSpaceId()).build())
                .setOwnerId(Common.UserId.newBuilder().setValue(dto.getOwnerId()).build())
                .setName(dto.getName() != null ? dto.getName() : "")
                .setDescription(dto.getDescription() != null ? dto.getDescription() : "");
        if (dto.getCollaboratorIds() != null) {
            builder.addAllCollaboratorIds(dto.getCollaboratorIds());
        }
        return builder.build();
    }

    private Crt.Asset toProtoAsset(AssetDTO dto) {
        return Crt.Asset.newBuilder()
                .setAssetId(dto.getAssetId())
                .setSpaceId(Common.SpaceId.newBuilder().setValue(dto.getSpaceId()).build())
                .setOwnerId(Common.UserId.newBuilder().setValue(dto.getOwnerId()).build())
                .setName(dto.getName() != null ? dto.getName() : "")
                .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
                .setVersion(dto.getVersion())
                .build();
    }

    private Crt.Template toProtoTemplate(TemplateDTO dto) {
        return Crt.Template.newBuilder()
                .setTemplateId(dto.getTemplateId())
                .setName(dto.getName() != null ? dto.getName() : "")
                .setDescription(dto.getDescription() != null ? dto.getDescription() : "")
                .setAuthorId(dto.getAuthorId() != null ? dto.getAuthorId() : "")
                .setThumbnailUrl(dto.getThumbnailUrl() != null ? dto.getThumbnailUrl() : "")
                .setUsageCount(dto.getUsageCount())
                .setRating(dto.getRating())
                .setIsOfficial(dto.isOfficial())
                .addAllTags(dto.getTags() != null ? dto.getTags() : List.of())
                .build();
    }

    private Crt.SceneGraph toProtoSceneGraph(ProjectConfig.SceneGraph sg) {
        if (sg == null) return Crt.SceneGraph.getDefaultInstance();
        return Crt.SceneGraph.newBuilder()
                .setRootNodeId(sg.getRootNodeId() != null ? sg.getRootNodeId() : "")
                .addAllNodes(sg.getNodes() != null ? sg.getNodes().stream()
                        .map(n -> Crt.SceneNode.newBuilder()
                                .setNodeId(n.getNodeId() != null ? n.getNodeId() : "")
                                .setParentId(n.getParentId() != null ? n.getParentId() : "")
                                .setName(n.getName() != null ? n.getName() : "")
                                .setAssetRef(n.getAssetRef() != null ? n.getAssetRef() : "")
                                .setVisible(n.isVisible())
                                .setLocked(n.isLocked())
                                .build())
                        .collect(Collectors.toList()) : List.of())
                .build();
    }

    private ProjectConfig.SceneGraph fromProtoSceneGraph(Crt.SceneGraph proto) {
        ProjectConfig.SceneGraph sg = new ProjectConfig.SceneGraph();
        sg.setRootNodeId(proto.getRootNodeId());
        sg.setNodes(proto.getNodesList().stream().map(n -> {
            ProjectConfig.SceneNode node = new ProjectConfig.SceneNode();
            node.setNodeId(n.getNodeId());
            node.setParentId(n.getParentId());
            node.setName(n.getName());
            node.setAssetRef(n.getAssetRef());
            node.setVisible(n.getVisible());
            node.setLocked(n.getLocked());
            return node;
        }).collect(Collectors.toList()));
        return sg;
    }

    private String extractUserId() {
        // TODO: Extract from gRPC metadata / auth interceptor
        return "default-user";
    }
}
