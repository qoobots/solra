package com.solra.not.infrastructure.persistence;

import com.solra.not.domain.model.InboxMessage;
import com.solra.not.domain.model.MessageStatus;
import com.solra.not.domain.model.MessageType;
import com.solra.not.domain.repository.InboxMessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class InboxMessageRepositoryImpl implements InboxMessageRepository {

    private final InboxMessageJpaRepository jpaRepo;

    public InboxMessageRepositoryImpl(InboxMessageJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public InboxMessage save(InboxMessage message) {
        return toDomain(jpaRepo.save(toEntity(message)));
    }

    @Override
    public Optional<InboxMessage> findById(String messageId) {
        return jpaRepo.findById(messageId).map(this::toDomain);
    }

    @Override
    public List<InboxMessage> findByRecipientId(String recipientId, int page, int size) {
        return jpaRepo.findByRecipientIdOrderBySentAtDesc(recipientId,
                        org.springframework.data.domain.PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InboxMessage> findUnreadByRecipientId(String recipientId) {
        return jpaRepo.findByRecipientIdAndStatusIn(recipientId,
                        List.of("SENT", "DELIVERED"))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countUnreadByRecipientId(String recipientId) {
        return jpaRepo.countByRecipientIdAndStatusIn(recipientId, List.of("SENT", "DELIVERED"));
    }

    @Override
    public List<InboxMessage> findByConversationId(String conversationId, int page, int size) {
        return jpaRepo.findByConversationIdOrderBySentAtDesc(conversationId,
                        org.springframework.data.domain.PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(String messageId) {
        jpaRepo.findById(messageId).ifPresent(e -> {
            e.setStatus("READ");
            e.setReadAt(java.time.Instant.now());
            jpaRepo.save(e);
        });
    }

    @Override
    public void markAllAsRead(String recipientId) {
        List<InboxMessageEntity> unread = jpaRepo.findByRecipientIdAndStatusIn(
                recipientId, List.of("SENT", "DELIVERED"));
        for (InboxMessageEntity e : unread) {
            e.setStatus("READ");
            e.setReadAt(java.time.Instant.now());
        }
        jpaRepo.saveAll(unread);
    }

    @Override
    public void deleteById(String messageId) {
        jpaRepo.deleteById(messageId);
    }

    private InboxMessage toDomain(InboxMessageEntity e) {
        InboxMessage m = new InboxMessage();
        m.setMessageId(e.getMessageId());
        m.setSenderId(e.getSenderId());
        m.setRecipientId(e.getRecipientId());
        m.setType(e.getType() != null ? MessageType.valueOf(e.getType()) : MessageType.TEXT);
        m.setStatus(e.getStatus() != null ? MessageStatus.valueOf(e.getStatus()) : MessageStatus.SENT);
        m.setTitle(e.getTitle());
        m.setContent(e.getContent());
        m.setAttachmentUrl(e.getAttachmentUrl());
        m.setMetadata(e.getMetadata());
        m.setConversationId(e.getConversationId());
        m.setSentAt(e.getSentAt());
        m.setReadAt(e.getReadAt());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }

    private InboxMessageEntity toEntity(InboxMessage m) {
        InboxMessageEntity e = new InboxMessageEntity();
        e.setMessageId(m.getMessageId());
        e.setSenderId(m.getSenderId());
        e.setRecipientId(m.getRecipientId());
        e.setType(m.getType() != null ? m.getType().name() : "TEXT");
        e.setStatus(m.getStatus() != null ? m.getStatus().name() : "SENT");
        e.setTitle(m.getTitle());
        e.setContent(m.getContent());
        e.setAttachmentUrl(m.getAttachmentUrl());
        e.setMetadata(m.getMetadata());
        e.setConversationId(m.getConversationId());
        e.setSentAt(m.getSentAt());
        e.setReadAt(m.getReadAt());
        e.setCreatedAt(m.getCreatedAt());
        return e;
    }
}
