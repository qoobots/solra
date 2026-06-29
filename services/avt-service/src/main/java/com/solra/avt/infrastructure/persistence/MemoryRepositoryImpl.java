package com.solra.avt.infrastructure.persistence;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MemoryRepositoryImpl implements MemoryRepository {

    private final MemoryJpaRepository jpa;

    public MemoryRepositoryImpl(MemoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<MemoryEntry> findByUserId(String userId, List<MemoryType> types, float minImportance, int maxResults) {
        List<String> typeNames = types.stream().map(Enum::name).collect(Collectors.toList());
        return jpa.findByUserIdAndTypes(userId, typeNames, minImportance, PageRequest.of(0, maxResults))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(MemoryEntry entry) {
        jpa.save(toEntity(entry));
    }

    @Override
    public void saveAll(List<MemoryEntry> entries) {
        jpa.saveAll(entries.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    private MemoryEntry toDomain(MemoryEntity e) {
        MemoryEntry m = new MemoryEntry();
        m.setMemoryId(e.getMemoryId());
        m.setUserId(e.getUserId());
        m.setConversationId(e.getConversationId());
        m.setType(MemoryType.valueOf(e.getType()));
        m.setContent(e.getContent());
        m.setImportance(e.getImportance());
        m.setCreatedAt(e.getCreatedAt());
        m.setLastAccessed(e.getLastAccessed());
        m.setExpiresAt(e.getExpiresAt());
        return m;
    }

    private MemoryEntity toEntity(MemoryEntry m) {
        MemoryEntity e = new MemoryEntity();
        e.setMemoryId(m.getMemoryId());
        e.setUserId(m.getUserId());
        e.setConversationId(m.getConversationId());
        e.setType(m.getType().name());
        e.setContent(m.getContent());
        e.setImportance(m.getImportance());
        e.setCreatedAt(m.getCreatedAt());
        e.setLastAccessed(m.getLastAccessed());
        e.setExpiresAt(m.getExpiresAt());
        return e;
    }
}
