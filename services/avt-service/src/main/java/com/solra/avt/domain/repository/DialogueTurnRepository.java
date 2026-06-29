package com.solra.avt.domain.repository;

import com.solra.avt.domain.model.DialogueTurn;
import java.util.List;

public interface DialogueTurnRepository {
    List<DialogueTurn> findByConversationId(String conversationId, int offset, int limit);
    void save(DialogueTurn turn);
    void saveAll(List<DialogueTurn> turns);
    long countByConversationId(String conversationId);
}
