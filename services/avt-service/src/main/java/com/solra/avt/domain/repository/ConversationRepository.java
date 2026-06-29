package com.solra.avt.domain.repository;

import com.solra.avt.domain.model.Conversation;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository {
    Optional<Conversation> findById(String conversationId);
    List<Conversation> findByUserId(String userId);
    void save(Conversation conversation);
    void deleteById(String conversationId);
}
