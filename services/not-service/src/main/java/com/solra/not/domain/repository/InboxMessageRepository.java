package com.solra.not.domain.repository;

import com.solra.not.domain.model.InboxMessage;

import java.util.List;
import java.util.Optional;

/**
 * 收件箱消息仓储接口。
 */
public interface InboxMessageRepository {
    InboxMessage save(InboxMessage message);
    Optional<InboxMessage> findById(String messageId);
    List<InboxMessage> findByRecipientId(String recipientId, int page, int size);
    List<InboxMessage> findUnreadByRecipientId(String recipientId);
    long countUnreadByRecipientId(String recipientId);
    List<InboxMessage> findByConversationId(String conversationId, int page, int size);
    void markAsRead(String messageId);
    void markAllAsRead(String recipientId);
    void deleteById(String messageId);
}
