package com.solra.soc.application.dto;

/**
 * 好友请求命令 — 可变的 JavaBean。
 */
public class FriendRequestCommand {

    private String userId;
    private String friendUserId;

    public FriendRequestCommand() {}

    public FriendRequestCommand(String userId, String friendUserId) {
        this.userId = userId;
        this.friendUserId = friendUserId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFriendUserId() { return friendUserId; }
    public void setFriendUserId(String friendUserId) { this.friendUserId = friendUserId; }
}
