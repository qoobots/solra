package com.solra.soc.application.dto;

/**
 * SessionCommand — SOC-001 会话相关命令对象。
 */
public class SessionCommand {

    private String spaceId;
    private String hostId;
    private String type;       // PRIVATE, FRIENDS, PUBLIC, EVENT
    private int maxParticipants;
    private boolean voiceChatEnabled = true;
    private boolean gestureEnabled = true;
    private boolean screenShareEnabled;
    private boolean recordSession;

    public SessionCommand() {}

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public boolean isVoiceChatEnabled() { return voiceChatEnabled; }
    public void setVoiceChatEnabled(boolean v) { this.voiceChatEnabled = v; }
    public boolean isGestureEnabled() { return gestureEnabled; }
    public void setGestureEnabled(boolean v) { this.gestureEnabled = v; }
    public boolean isScreenShareEnabled() { return screenShareEnabled; }
    public void setScreenShareEnabled(boolean v) { this.screenShareEnabled = v; }
    public boolean isRecordSession() { return recordSession; }
    public void setRecordSession(boolean v) { this.recordSession = v; }
}

/**
 * JoinSessionCommand — SOC-001 加入会话命令。
 */
public class JoinSessionCommand {
    private String sessionId;
    private String userId;
    private String avatarId;
    private String webrtcAnswerSdp;

    public JoinSessionCommand() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }
    public String getUserId() { return userId; }
    public void setUserId(String id) { this.userId = id; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String id) { this.avatarId = id; }
    public String getWebrtcAnswerSdp() { return webrtcAnswerSdp; }
    public void setWebrtcAnswerSdp(String sdp) { this.webrtcAnswerSdp = sdp; }
}

/**
 * SendMessageCommand — SOC-002 发送消息命令。
 */
public class SendMessageCommand {
    private String sessionId;
    private String fromUserId;
    private String toUserId;
    private String type;       // TEXT, VOICE, SYSTEM
    private String content;
    private String voiceUrl;
    private int voiceDurationMs;

    public SendMessageCommand() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String id) { this.fromUserId = id; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String id) { this.toUserId = id; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String u) { this.voiceUrl = u; }
    public int getVoiceDurationMs() { return voiceDurationMs; }
    public void setVoiceDurationMs(int ms) { this.voiceDurationMs = ms; }
}

/**
 * OnlineParticipantsDTO — 在线参与者列表。
 */
public record OnlineParticipantsDTO(java.util.List<SessionDTO.ParticipantDTO> participants, int totalOnline) {}
