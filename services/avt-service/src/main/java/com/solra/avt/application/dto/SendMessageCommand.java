package com.solra.avt.application.dto;

import com.solra.avt.domain.model.MessageAttachment;
import java.util.List;
import java.util.Map;

public class SendMessageCommand {
    private String userId;
    private String spaceId;
    private String conversationId;
    private String content;
    private List<MessageAttachment> attachments;
    private Map<String, String> context;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<MessageAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<MessageAttachment> attachments) { this.attachments = attachments; }
    public Map<String, String> getContext() { return context; }
    public void setContext(Map<String, String> context) { this.context = context; }
}
