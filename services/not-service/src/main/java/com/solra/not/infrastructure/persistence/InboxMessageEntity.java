package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inbox_messages")
public class InboxMessageEntity {
    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;
    @Column(name = "sender_id", length = 64)
    private String senderId;
    @Column(name = "recipient_id", length = 64, nullable = false)
    private String recipientId;
    @Column(name = "type", length = 20, nullable = false)
    private String type;
    @Column(name = "status", length = 20, nullable = false)
    private String status;
    @Column(name = "title", length = 256)
    private String title;
    @Column(name = "content", length = 4096)
    private String content;
    @Column(name = "attachment_url", length = 1024)
    private String attachmentUrl;
    @Column(name = "metadata", length = 1024)
    private String metadata;
    @Column(name = "conversation_id", length = 64)
    private String conversationId;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "created_at")
    private Instant createdAt;

    public InboxMessageEntity() {}

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
