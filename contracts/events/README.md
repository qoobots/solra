# Solra Event Schema

> 跨服务领域事件定义，采用 Apache Avro 格式。事件通过 Kafka 发布/订阅。

## 目录结构

```
events/
├── README.md              ← 本文件
├── space-events.avsc      ← 空间域: SpaceEntered
├── user-events.avsc       ← 用户域: UserLeftSpace
├── avatar-events.avsc     ← 虚拟人域: AvatarStateChanged, ConversationMessage
├── social-events.avsc     ← 社交域: UserPresenceChanged, SocialInteraction
└── growth-events.avsc     ← 成长域: AchievementUnlocked, FaithLevelChanged
```

## 事件分类

| 域 | 事件 | 生产者 | 消费者 |
|----|------|--------|--------|
| space | SpaceEntered | avt-service | soc-service, spc-service, not-service |
| user | UserLeftSpace | avt-service | soc-service, spc-service, grw-service |
| avatar | AvatarStateChanged | avt-service | soc-service, crt-service |
| avatar | ConversationMessage | avt-service | saf-service, grw-service |
| social | UserPresenceChanged | soc-service | avt-service, not-service |
| social | SocialInteraction | soc-service | grw-service, not-service, saf-service |
| growth | AchievementUnlocked | grw-service | not-service, crt-service |
| growth | FaithLevelChanged | grw-service | avt-service, not-service |

## Schema 演进规则

- 新增字段：须设 `default` 值（向后兼容）
- 禁止：重命名已有字段、删除必填字段
- 枚举：只允许追加新 symbol，不得删除或重排序

## 工具

```bash
# 编译 Schema 为 Java/Python
avro-tools compile schema events/*.avsc output/
```
