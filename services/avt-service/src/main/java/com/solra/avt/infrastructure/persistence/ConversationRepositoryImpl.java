package com.solra.avt.infrastructure.persistence;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationJpaRepository jpa;

    public ConversationRepositoryImpl(ConversationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Conversation> findById(String conversationId) {
        return jpa.findById(conversationId).map(this::toDomain);
    }

    @Override
    public List<Conversation> findByUserId(String userId) {
        return jpa.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(Conversation conversation) {
        jpa.save(toEntity(conversation));
    }

    @Override
    public void deleteById(String conversationId) {
        jpa.deleteById(conversationId);
    }

    private Conversation toDomain(ConversationEntity e) {
        Conversation c = new Conversation();
        c.setConversationId(e.getConversationId());
        c.setUserId(e.getUserId());
        c.setSpaceId(e.getSpaceId());
        c.setAvatarId(e.getAvatarId());
        c.setStatus(ConversationStatus.valueOf(e.getStatus()));
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        c.setMetadata(e.getMetadata());
        return c;
    }

    private ConversationEntity toEntity(Conversation c) {
        ConversationEntity e = new ConversationEntity();
        e.setConversationId(c.getConversationId());
        e.setUserId(c.getUserId());
        e.setSpaceId(c.getSpaceId());
        e.setAvatarId(c.getAvatarId());
        e.setStatus(c.getStatus().name());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        e.setMetadata(c.getMetadata());
        return e;
    }
}
