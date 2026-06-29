package com.solra.not.domain.model;

import java.time.Instant;

/**
 * InboxMessage — 收件箱消息聚合根。
 * 应用内消息中心的消息实体，支持多种消息类型和富媒体附件。
 * NOT-002: 应用内消息中心。
 */
public class InboxMessage {

    private String messageId;
    private String senderId;
    private String recipientId;
    private MessageType type;
    private MessageStatus status;
    private String title;
    private String content;
    private String attachmentUrl;
    private String metadata;
    private String conversationId;
    private Instant sentAt;
    private Instant readAt;
    private Instant createdAt;

    public InboxMessage() {}

    public InboxMessage(String messageId, String senderId, String recipientId,
                         MessageType type, String title, String content) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.type = type;
        this.status = MessageStatus.SENT;
        this.title = title;
        this.content = content;
        this.sentAt = Instant.now();
        this.createdAt = this.sentAt;
    }

    /** 标记为已送达 */
    public void markDelivered() {
        this.status = MessageStatus.DELIVERED;
    }

    /** 标记为已读 */
    public void markRead() {
        this.status = MessageStatus.READ;
        this.readAt = Instant.now();
    }

    /** 是否未读 */
    public boolean isUnread() {
        return status == MessageStatus.SENT || status == MessageStatus.DELIVERED;
    }

    // ---- getters/setters ----

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
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
