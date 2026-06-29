package com.solra.spc.domain.repository;

import com.solra.spc.domain.model.Space;
import com.solra.spc.domain.model.SpaceCategory;
import java.util.List;
import java.util.Optional;

public interface SpaceRepository {
    Optional<Space> findById(String spaceId);
    List<Space> findByIds(List<String> spaceIds);
    List<Space> findPublished(int offset, int limit, List<SpaceCategory> categories, String sortBy);
    long countPublished(List<SpaceCategory> categories);
    void save(Space space);
    void incrementViewCount(String spaceId);
    void incrementLikeCount(String spaceId);
    void incrementShareCount(String spaceId);
}
