package com.solra.avt.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, String> {
    List<ConversationEntity> findByUserIdOrderByUpdatedAtDesc(String userId);
}

@Repository
interface DialogueTurnJpaRepository extends JpaRepository<DialogueTurnEntity, String> {
    @Query("SELECT t FROM DialogueTurnEntity t WHERE t.conversationId = :convId ORDER BY t.timestamp ASC")
    List<DialogueTurnEntity> findPageByConversationId(String convId, org.springframework.data.domain.Pageable pageable);

    long countByConversationId(String conversationId);
}

@Repository
interface MemoryJpaRepository extends JpaRepository<MemoryEntity, String> {
    @Query("SELECT m FROM MemoryEntity m WHERE m.userId = :userId " +
           "AND m.type IN :types AND m.importance >= :minImportance ORDER BY m.lastAccessed DESC")
    List<MemoryEntity> findByUserIdAndTypes(String userId, List<String> types, float minImportance,
                                            org.springframework.data.domain.Pageable pageable);
}
