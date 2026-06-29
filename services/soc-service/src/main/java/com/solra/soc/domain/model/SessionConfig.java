package com.solra.soc.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * SessionConfig — 多人会话配置值对象 (SOC-001/SOC-006).
 */
public class SessionConfig {

    private boolean voiceChatEnabled = true;
    private boolean gestureEnabled = true;
    private boolean screenShareEnabled = false;
    private boolean recordSession = false;
    private List<String> entryApprovalUserIds = new ArrayList<>();

    public static SessionConfig defaults() {
        return new SessionConfig();
    }

    public static SessionConfig voiceOnly() {
        SessionConfig c = new SessionConfig();
        c.gestureEnabled = false;
        return c;
    }

    public static SessionConfig fullFeatures() {
        SessionConfig c = new SessionConfig();
        c.screenShareEnabled = true;
        return c;
    }

    // -- Getters/Setters --
    public boolean isVoiceChatEnabled() { return voiceChatEnabled; }
    public void setVoiceChatEnabled(boolean voiceChatEnabled) { this.voiceChatEnabled = voiceChatEnabled; }
    public boolean isGestureEnabled() { return gestureEnabled; }
    public void setGestureEnabled(boolean gestureEnabled) { this.gestureEnabled = gestureEnabled; }
    public boolean isScreenShareEnabled() { return screenShareEnabled; }
    public void setScreenShareEnabled(boolean screenShareEnabled) { this.screenShareEnabled = screenShareEnabled; }
    public boolean isRecordSession() { return recordSession; }
    public void setRecordSession(boolean recordSession) { this.recordSession = recordSession; }
    public List<String> getEntryApprovalUserIds() { return entryApprovalUserIds; }
    public void setEntryApprovalUserIds(List<String> entryApprovalUserIds) { this.entryApprovalUserIds = entryApprovalUserIds; }
}
