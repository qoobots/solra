package com.solra.avt.domain.repository;

import com.solra.avt.domain.model.MemoryEntry;
import com.solra.avt.domain.model.MemoryType;
import java.util.List;

public interface MemoryRepository {
    List<MemoryEntry> findByUserId(String userId, List<MemoryType> types, float minImportance, int maxResults);
    void save(MemoryEntry entry);
    void saveAll(List<MemoryEntry> entries);
}
