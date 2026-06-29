package com.solra.crt.domain.repository;

import com.solra.crt.domain.entity.SpaceProject;
import java.util.List;
import java.util.Optional;

/**
 * 空间项目仓储接口（领域层端口）。
 */
public interface ProjectRepository {

    Optional<SpaceProject> findById(String projectId);

    List<SpaceProject> findByOwnerId(String ownerId);

    List<SpaceProject> findBySpaceId(String spaceId);

    List<SpaceProject> findByOwnerIdAndStatus(String ownerId, SpaceProject.ProjectStatus status,
                                               int offset, int limit);

    long countByOwnerIdAndStatus(String ownerId, SpaceProject.ProjectStatus status);

    SpaceProject save(SpaceProject project);

    void deleteById(String projectId);
}
