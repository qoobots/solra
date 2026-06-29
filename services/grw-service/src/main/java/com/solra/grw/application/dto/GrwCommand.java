package com.solra.grw.application.dto;

/** 记录经验命令 */
public class RecordExperienceCommand {
    private String userId;
    private String eventType;
    private int value;
    private String spaceId;
    private String metadata;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

/** 检测决定性时刻命令 */
public record DetectMomentsCommand(String userId, java.util.List<String> recentActions,
                                    java.util.Map<String, Object> currentState) {}

/** 评估流失风险命令 */
public class EvaluateChurnRiskCommand {
    private String userId;
    private String avatarName;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAvatarName() { return avatarName; }
    public void setAvatarName(String avatarName) { this.avatarName = avatarName; }
}

/** 处理召回回调命令 */
public class RecallCallbackCommand {
    private String taskId;
    private String action; // CLICKED, CONVERTED, EXPIRED, CANCELLED

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}

/** 批量评估流失风险命令 */
public record BatchChurnEvaluationCommand(java.util.List<String> userIds) {}

/** 增加经验值命令 (GRW-001) */
public record AddExperienceCommand(String userId, int amount, String eventType) {}

/** 布道者申请命令 (GRW-003) */
public record EvangelistApplyCommand(String userId, String displayName, String bio) {}

/** 布道者审批命令 (GRW-003) */
public record EvangelistReviewCommand(String applicationId, boolean approved,
                                       String reviewerId, String comment) {}

/** 收集虚拟人命令 (GRW-004) */
public record CollectAvatarCommand(String userId, String avatarTypeId, String name,
                                    String rarity, String element) {}

/** 虚拟人养成命令 (GRW-004) */
public record AvatarExperienceCommand(String userId, String avatarTypeId, int amount) {}

/** 虚拟人好感度命令 (GRW-004) */
public record AvatarAffectionCommand(String userId, String avatarTypeId, int amount) {}

/** 信仰仪表盘生成命令 (GRW-008) */
public record GenerateDashboardCommand(String userId) {}
