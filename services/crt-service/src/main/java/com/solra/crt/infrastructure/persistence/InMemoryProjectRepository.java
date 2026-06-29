package com.solra.crt.infrastructure.persistence;

import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.repository.ProjectRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 项目仓储内存实现（开发阶段，后续替换为 JPA/PostgreSQL）。
 */
public class InMemoryProjectRepository implements ProjectRepository {

    private final Map<String, SpaceProject> store = new ConcurrentHashMap<>();

    @Override
    public Optional<SpaceProject> findById(String projectId) {
        return Optional.ofNullable(store.get(projectId));
    }

    @Override
    public List<SpaceProject> findByOwnerId(String ownerId) {
        return store.values().stream()
                .filter(p -> ownerId.equals(p.getOwnerId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceProject> findBySpaceId(String spaceId) {
        return store.values().stream()
                .filter(p -> spaceId.equals(p.getSpaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceProject> findByOwnerIdAndStatus(String ownerId,
                                                      SpaceProject.ProjectStatus status,
                                                      int offset, int limit) {
        return store.values().stream()
                .filter(p -> ownerId.equals(p.getOwnerId()))
                .filter(p -> status == null || p.getStatus() == status)
                .sorted(Comparator.comparing(SpaceProject::getUpdatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long countByOwnerIdAndStatus(String ownerId, SpaceProject.ProjectStatus status) {
        return store.values().stream()
                .filter(p -> ownerId.equals(p.getOwnerId()))
                .filter(p -> status == null || p.getStatus() == status)
                .count();
    }

    @Override
    public SpaceProject save(SpaceProject project) {
        store.put(project.getProjectId(), project);
        return project;
    }

    @Override
    public void deleteById(String projectId) {
        store.remove(projectId);
    }
}
