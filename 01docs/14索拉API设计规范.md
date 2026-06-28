# 索拉（Solra）API 设计规范文档

> 跨 10 个子项目、4 种通信协议的统一 API 设计标准——REST/gRPC/WebSocket/消息事件的命名约定、版本策略、错误码体系与文档自动化

---

## 一、API 设计总纲

### 1.1 文档定位

`07索拉应用架构设计.md` 定义了子项目间的通信模式（同步 REST/gRPC + 异步事件），`08索拉技术架构设计.md` 给出了协议选型。本文档统一定义**所有对外/对内 API 的设计规范**，确保：

- **一致性**：10 个子项目的 API 遵循相同的命名、分页、错误格式
- **向前兼容**：版本策略保障老客户端不因 API 变更而中断
- **自动化文档**：从 Protobuf / OpenAPI 自动生成文档，不做手工维护
- **端云协议共享**：客户端与服务的 Protobuf 定义同源，杜绝不一致

```
API 规范文档层级：
┌──────────────────────────────────────────────────────────┐
│  07 应用架构设计    → 通信模式 (REST/gRPC/Event/WS)       │
│  08 技术架构设计    → 协议选型 (gRPC/Protobuf/APISIX)     │
│  14 API 设计规范    → 命名+版本+错误码+安全+文档自动化     │  ← 本文档
└──────────────────────────────────────────────────────────┘
```

### 1.2 API 设计原则

| 原则 | 说明 | 反例 |
|------|------|------|
| **资源导向** | API 围绕资源（名词）设计，而非操作（动词） | `/getUserInfo` ❌ |
| **协议一致** | 同类服务使用相同协议，不混用 | 一半 REST 一半 gRPC 做同样的事 |
| **向前兼容** | 新增字段不破坏老客户端；废弃字段不立即删除 | 直接改字段类型导致老端 crash |
| **分页必选** | 列表接口默认分页，防止全量查询 | 列表接口无分页导致 DB 打爆 |
| **幂等设计** | 写操作支持幂等（幂等键），防止重复提交 | 支付接口不支持幂等导致重复扣款 |
| **错误可操作** | 错误信息包含 machine-readable code + human-readable message | `{"error":"something wrong"}` ❌ |

### 1.3 协议选择策略

```
通信场景决策矩阵：

┌────────────────────────┬──────────────┬──────────────────────────┐
│ 场景                    │ 协议          │ 理由                      │
├────────────────────────┼──────────────┼──────────────────────────┤
│ 客户端 → 服务端（CRUD） │ REST/HTTP2    │ 浏览器友好、缓存/CDN       │
│ 客户端 → 服务端（高频） │ WebSocket    │ 实时双向通信               │
│ 微服务间（同步调用）    │ gRPC         │ 高性能、强类型、流式        │
│ 微服务间（异步事件）    │ Pulsar Msg   │ 解耦、事件溯源              │
│ 文件上传/下载           │ REST Multipart│ 分块上传/断点续传          │
│ 实时状态同步            │ WebRTC       │ P2P 低延迟                 │
│ 第三方集成              │ REST + Webhook│ 最广泛的生态支持           │
└────────────────────────┴──────────────┴──────────────────────────┘
```

---

## 二、Protobuf 规范（服务间通信标准）

### 2.1 仓库结构

```
protobuf/
├── buf.yaml                    # Buf 配置（lint规则 + breaking change检测）
├── buf.gen.yaml                # 代码生成配置
├── solra/
│   ├── common/
│   │   ├── v1/
│   │   │   ├── types.proto     # 通用类型（UserId, Timestamp, GeoPoint...）
│   │   │   ├── pagination.proto # 分页
│   │   │   └── error.proto     # 错误码枚举
│   │   └── v2/                 # 未来版本
│   ├── avt/
│   │   └── v1/
│   │       ├── conversation.proto
│   │       ├── memory.proto
│   │       └── emotion.proto
│   ├── spc/
│   │   └── v1/
│   │       ├── space.proto
│   │       └── recommendation.proto
│   ├── crt/
│   │   └── v1/
│   │       └── creation.proto
│   ├── soc/
│   │   └── v1/
│   │       └── presence.proto
│   ├── grw/
│   │   └── v1/
│   │       └── growth.proto
│   ├── mon/
│   │   └── v1/
│   │       └── monetization.proto
│   ├── auth/
│   │   └── v1/
│   │       └── auth.proto
│   ├── saf/
│   │   └── v1/
│   │       └── safety.proto
│   └── not/
│       └── v1/
│           └── notification.proto
└── third_party/
    └── google/
        └── api/
            └── annotations.proto
```

### 2.2 Protobuf 编写规范

```protobuf
syntax = "proto3";

package solra.avt.v1;

import "solra/common/v1/types.proto";
import "solra/common/v1/pagination.proto";

option java_package = "io.solra.proto.avt.v1";
option java_multiple_files = true;

// ============================================================
// 虚拟人对话服务
// ============================================================
service ConversationService {
  // 发送对话消息（双向流式）
  // 客户端流式发送音频/文本片段，服务端流式返回虚拟人回复
  rpc SendMessage(stream SendMessageRequest) returns (stream SendMessageResponse);

  // 获取历史对话
  rpc ListHistory(ListHistoryRequest) returns (ListHistoryResponse);

  // 标记对话已读
  rpc MarkRead(MarkReadRequest) returns (MarkReadResponse);
}

// ---------- SendMessage ----------
message SendMessageRequest {
  // 请求体：只能选择 text 或 audio_data 之一
  oneof content {
    string text = 1;                   // 文本消息
    bytes audio_data = 2;              // 音频数据 (PCM 16kHz)
  }

  solra.common.v1.UserId user_id = 3;   // 用户ID
  string space_id = 4;                  // 所在空间ID
  string avatar_id = 5;                 // 对话虚拟人ID
  string conversation_id = 6;           // 对话会话ID (首次为空，服务端创建)
  AudioConfig audio_config = 7;         // 音频配置 (首次发送)
  string idempotency_key = 8;           // 幂等键
}

message AudioConfig {
  int32 sample_rate = 1;  // 采样率 (默认 16000)
  string encoding = 2;    // 编码格式 (pcm_s16le / opus)
}

message SendMessageResponse {
  // 响应体：始终包含 text，可选包含 audio_data
  string text = 1;                    // 虚拟人文本回复（始终返回）
  bytes audio_data = 2;               // 虚拟人语音回复 (TTS 输出)
  string conversation_id = 3;         // 对话会话ID

  // 虚拟人当前状态
  AvatarState avatar_state = 4;

  // 对话分析
  IntentDetected intent = 5;          // 意图识别
  EmotionState emotion = 6;           // 当前情感状态
  repeated MemoryRecall memories = 7; // 召回的相关记忆
}

message AvatarState {
  string animation_state = 1;  // 动画状态: idle / talking / thinking / surprised / waving
  string expression = 2;       // 表情: smile / curious / neutral / shy / laugh
  Position3D position = 3;     // 3D 位置
  Rotation3D rotation = 4;     // 3D 朝向
}

message IntentDetected {
  string intent = 1;       // 意图分类: greeting / question / sharing / command / goodbye
  float confidence = 2;    // 置信度 [0, 1]
}

message EmotionState {
  float joy = 1;         // 开心 [0, 1]
  float curiosity = 2;   // 好奇 [0, 1]
  float coldness = 3;    // 冷淡 [0, 1]
  float jealousy = 4;    // 吃醋 [0, 1]
  float sadness = 5;     // 失落 [0, 1]
}

message MemoryRecall {
  string memory_id = 1;
  string topic = 2;
  float relevance_score = 3;   // 相关性评分 [0, 1]
  string recalled_from = 4;    // 召回日期
}

// ---------- ListHistory ----------
message ListHistoryRequest {
  solra.common.v1.UserId user_id = 1;
  string avatar_id = 2;
  solra.common.v1.Pagination page = 3;  // 分页
}

message ListHistoryResponse {
  repeated DialogueTurn turns = 1;
  solra.common.v1.PageInfo page_info = 2;
}

message DialogueTurn {
  string turn_id = 1;
  string user_text = 2;
  string avatar_text = 3;
  google.protobuf.Timestamp created_at = 4;
}

// ---------- MarkRead ----------
message MarkReadRequest {
  solra.common.v1.UserId user_id = 1;
  string conversation_id = 2;
  string last_read_turn_id = 3;   // 最后已读的 turn id
}

message MarkReadResponse {
  bool success = 1;
}
```

### 2.3 通用类型定义

```protobuf
// solra/common/v1/types.proto
syntax = "proto3";
package solra.common.v1;

// 用户ID（封装字符串，避免裸 string 传递）
message UserId {
  string value = 1;  // 格式: user_{snowflake_id}
}

// 空间ID
message SpaceId {
  string value = 1;  // 格式: space_{snowflake_id}
}

// 虚拟人ID
message AvatarId {
  string value = 1;  // 格式: avatar_{snowflake_id}
}

// 3D 位置
message Position3D {
  float x = 1;
  float y = 2;
  float z = 3;
}

// 3D 旋转
message Rotation3D {
  float pitch = 1;  // 俯仰角 (弧度)
  float yaw = 2;    // 偏航角 (弧度)
  float roll = 3;   // 翻滚角 (弧度)
}

// 地理坐标
message GeoPoint {
  double latitude = 1;
  double longitude = 2;
}

// 时间范围
message TimeRange {
  google.protobuf.Timestamp start = 1;
  google.protobuf.Timestamp end = 2;
}

// solra/common/v1/pagination.proto
message Pagination {
  int32 page = 1;       // 页码（从1开始），默认1
  int32 page_size = 2;  // 每页条数，默认20，最大100
  string cursor = 3;    // 游标分页（与 page/page_size 二选一）
}

message PageInfo {
  int32 total_count = 1;
  int32 total_pages = 2;
  int32 current_page = 3;
  int32 page_size = 4;
  bool has_next = 5;
  string next_cursor = 6;  // 游标分页时返回
}
```

### 2.4 Proto 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| Package | `solra.{子项目}.{版本}` | `solra.avt.v1` |
| Service | PascalCase + `Service` 后缀 | `ConversationService` |
| RPC Method | PascalCase 动词开头 | `SendMessage` / `ListHistory` |
| Message | PascalCase | `SendMessageRequest` / `SendMessageResponse` |
| 请求/响应 | Request / Response 后缀 | `ListHistoryRequest` |
| Field | snake_case | `user_id`, `created_at` |
| Enum | UPPER_SNAKE_CASE | `MESSAGE_TYPE_TEXT` |
| Oneof | snake_case 描述选择 | `content` |
| 时间字段 | `google.protobuf.Timestamp` | `created_at` |
| 货币字段 | `int64` 分单位 | `amount_cents = 1` |

### 2.5 Breaking Change 检测

使用 `buf breaking` 在 CI 中检测不兼容变更：

| 禁止的 Breaking Change | 说明 |
|----------------------|------|
| 删除或重命名 Service/RPC | 客户端代码编译失败 |
| 删除或重命名 Field | 老客户端发送的字段丢失 |
| 修改 Field 类型 | 序列化不兼容 |
| 修改 Field Number | 有线格式不兼容 |
| 删除 Enum 值 | 可能导致未知值处理错误 |

---

## 三、REST API 规范

### 3.1 URL 设计

```
基础格式: https://api.solra.io/v1/{resource}

资源命名规则：
- 全部小写
- 使用名词复数
- 使用连字符 (kebab-case) 分隔多单词资源
- 最多三级嵌套

正确示例 ✅:
  GET    /v1/spaces                          # 空间列表
  GET    /v1/spaces/{space_id}               # 单个空间
  POST   /v1/spaces/{space_id}/visits        # 记录访次
  GET    /v1/spaces/{space_id}/avatars       # 空间下虚拟人列表
  GET    /v1/users/me                        # 当前用户
  PATCH  /v1/users/me/profile               # 更新当前用户资料

错误示例 ❌:
  GET    /v1/getSpaces                       # 不用动词
  POST   /v1/space                           # 不用单数
  GET    /v1/Spaces                          # 不用大写
  GET    /v1/spaces/{id}/avatars/{aid}/messages/{mid}  # 嵌套过深
```

### 3.2 HTTP 方法语义

| 方法 | 语义 | 幂等 | 安全 | 请求体 |
|------|------|------|------|--------|
| GET | 读取资源 | ✅ | ✅ | 无 |
| POST | 创建资源 | ❌ | ❌ | 有 |
| PUT | 全量替换资源 | ✅ | ❌ | 有 |
| PATCH | 部分更新资源 | ❌ | ❌ | 有 (JSON Merge Patch) |
| DELETE | 删除资源 | ✅ | ❌ | 无 |

### 3.3 请求与响应格式

#### 请求头

```http
POST /v1/spaces HTTP/2
Host: api.solra.io
Content-Type: application/json
Authorization: Bearer {access_token}
Idempotency-Key: {uuid}          # 幂等键（POST/PUT/PATCH 必须）
X-Request-Id: {uuid}             # 请求追踪ID
X-Client-Version: 1.2.0          # 客户端版本
X-Device-Id: {device_id}         # 设备标识
Accept-Language: zh-CN           # 语言偏好
```

#### 成功响应格式

```json
// 单个资源
{
  "data": {
    "id": "space_1234567890",
    "type": "space",
    "attributes": {
      "name": "海边咖啡馆",
      "creator_id": "user_111",
      "status": "published",
      "visit_count": 3241,
      "created_at": "2026-06-01T10:00:00Z"
    }
  }
}

// 列表资源
{
  "data": [
    { "id": "space_1", "type": "space", "attributes": {...} },
    { "id": "space_2", "type": "space", "attributes": {...} }
  ],
  "meta": {
    "pagination": {
      "page": 1,
      "page_size": 20,
      "total_count": 156,
      "total_pages": 8
    }
  }
}

// 创建成功
// HTTP 201 Created
// Location: /v1/spaces/space_123
{
  "data": { "id": "space_123", "type": "space", "attributes": {...} }
}

// 异步操作 (202 Accepted)
{
  "data": {
    "id": "job_publish_abc",
    "type": "async_job",
    "attributes": {
      "status": "processing",
      "estimated_seconds": 30
    }
  },
  "links": {
    "self": "/v1/jobs/job_publish_abc"
  }
}
```

### 3.4 分页规范

| 分页方式 | 适用场景 | 参数 | 示例 |
|---------|---------|------|------|
| 页码分页 | 需要总数、跳页浏览 | `page`, `page_size` | `?page=1&page_size=20` |
| 游标分页 | Feed流、实时更新 | `cursor`, `limit` | `?cursor=abc123&limit=20` |

```json
// 页码分页响应
{
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total_count": 1240,
    "total_pages": 62
  }
}

// 游标分页响应
{
  "pagination": {
    "next_cursor": "def456",
    "has_more": true,
    "limit": 20
  }
}
```

### 3.5 排序与过滤

```
过滤: ?filter[status]=published&filter[category]=cafe
排序: ?sort=-created_at (降序), ?sort=visit_count (升序)
搜索: ?q=海边的咖啡
包含关联: ?include=creator,avatars (减少 N+1)

多值过滤: ?filter[status]=published,draft
范围过滤: ?filter[created_at][gte]=2026-01-01&filter[created_at][lte]=2026-06-01
```

---

## 四、错误码体系

### 4.1 HTTP 状态码使用

| 状态码 | 语义 | 使用场景 |
|--------|------|---------|
| 200 OK | 成功 | GET、PATCH、PUT 成功 |
| 201 Created | 创建成功 | POST 创建新资源 |
| 202 Accepted | 异步处理中 | 长时间操作（发布空间审核） |
| 204 No Content | 成功无返回体 | DELETE 成功 |
| 301 Moved Permanently | 永久重定向 | API 版本迁移 |
| 400 Bad Request | 请求参数错误 | 参数校验失败 |
| 401 Unauthorized | 未认证 | Token 缺失/无效 |
| 403 Forbidden | 无权限 | 有 Token 但无权限操作 |
| 404 Not Found | 资源不存在 | 空间/用户/虚拟人 不存在 |
| 409 Conflict | 资源冲突 | 重复创建/版本冲突 |
| 422 Unprocessable Entity | 语义错误 | 参数合法但业务逻辑不满足 |
| 429 Too Many Requests | 限流 | 超过频率限制 |
| 500 Internal Server Error | 服务器错误 | 未预期异常 |
| 502 Bad Gateway | 上游错误 | 依赖服务不可用 |
| 503 Service Unavailable | 服务维护中 | 降级/维护 |

### 4.2 错误响应格式

```json
{
  "error": {
    "code": "SPACE_NOT_FOUND",
    "message": "空间不存在或已删除",
    "details": [
      {
        "field": "space_id",
        "reason": "空间 ID 'space_000' 不存在于当前用户可见范围",
        "recovery_suggestion": "请检查空间ID是否正确，或该空间是否已被删除"
      }
    ],
    "request_id": "req_a1b2c3d4",
    "documentation_url": "https://docs.solra.io/errors/SPACE_NOT_FOUND"
  }
}
```

### 4.3 错误码命名规范

```
错误码格式: {SERVICE}_{ERROR_TYPE}

SERVICE（服务前缀）:
  AUTH    - 身份认证    SPC     - 空间消费    AVT     - 虚拟人交互
  CRT     - 空间创作    SOC     - 社交在场    GRW     - 用户成长
  MON     - 商业化      SAF     - 内容安全    NOT     - 通知消息
  INF     - 基础设施    COMMON  - 通用错误

ERROR_TYPE（错误类型）:
  NOT_FOUND         - 资源不存在
  INVALID_PARAM     - 参数校验失败
  PERMISSION_DENIED - 权限不足
  QUOTA_EXCEEDED    - 配额超限
  RATE_LIMITED      - 频率限制
  CONFLICT          - 资源冲突
  INTERNAL_ERROR    - 内部错误
  SERVICE_UNAVAIL   - 服务不可用
```

### 4.4 标准错误码列表

| 错误码 | HTTP | 场景 |
|--------|------|------|
| COMMON_INVALID_PARAM | 400 | 通用参数校验失败 |
| COMMON_RATE_LIMITED | 429 | 通用频率限制 |
| AUTH_TOKEN_EXPIRED | 401 | AccessToken 过期 |
| AUTH_TOKEN_INVALID | 401 | Token 无效签名 |
| AUTH_REFRESH_TOKEN_REUSED | 401 | RefreshToken 被重复使用（泄漏检测） |
| AUTH_PERMISSION_DENIED | 403 | 无操作权限 |
| AUTH_REAL_NAME_REQUIRED | 403 | 需要实名认证 |
| AUTH_MINOR_RESTRICTED | 403 | 未成年人功能限制 |
| SPC_SPACE_NOT_FOUND | 404 | 空间不存在 |
| SPC_SPACE_NOT_LOADED | 503 | 空间资产加载失败 |
| AVT_AVATAR_NOT_AVAILABLE | 503 | 虚拟人推理服务不可用 |
| CRT_PUBLISH_REVIEW_REQUIRED | 403 | 空间需要审核才能发布 |
| MON_PAYMENT_FAILED | 402 | 支付失败 |
| MON_INSUFFICIENT_BALANCE | 402 | 余额不足 |
| SOC_SESSION_FULL | 409 | 空间人数已满 |
| SAF_CONTENT_BLOCKED | 403 | 内容被安全拦截 |

---

## 五、WebSocket 规范

### 5.1 连接建立

```
连接: wss://ws.solra.io/v1/ws
认证: URL 参数 ?token={access_token}
心跳: 客户端每 30 秒发送 ping，服务端回复 pong，60 秒无心跳断开
重连: 指数退避 1s→2s→4s→8s→最大30s，jitter ±20%
```

### 5.2 消息格式

```json
{
  "type": "message_type",
  "seq": 12345,           // 消息序号（客户端维护，递增）
  "timestamp": "2026-06-29T10:30:00.000Z",
  "payload": {
    // type 相关的业务数据
  }
}
```

### 5.3 WebSocket 消息类型

| Type | 方向 | 说明 |
|------|------|------|
| `space.enter` | C→S | 进入空间 |
| `space.leave` | C→S | 离开空间 |
| `space.presenters` | S→C | 空间内其他用户在场状态 |
| `space.render_state` | S→C | 空间渲染状态更新 |
| `chat.message` | C↔S | 空间内文字聊天 |
| `avatar.greeting` | S→C | 虚拟人主动打招呼 |
| `avatar.response` | S→C | 虚拟人回复（流式） |
| `social.gesture` | C↔S | 社交信号（举手/鼓掌等） |
| `sync.position` | C↔S | 位置同步 |
| `sync.action` | C↔S | 动作同步 |
| `system.notification` | S→C | 系统通知 |
| `system.error` | S→C | 服务端错误 |
| `ping` | C→S | 心跳请求 |
| `pong` | S→C | 心跳响应 |

### 5.4 流式响应协议

虚拟人对话使用 WebSocket 流式返回：

```json
// 用户发送文本
{"type":"chat.message","seq":1,"timestamp":"...","payload":{"text":"你好"}}

// 服务端流式返回（多个 chunk）
{"type":"avatar.response","seq":1,"timestamp":"...","payload":{"chunk":"嗨！","is_final":false,"conversation_id":"conv_123"}}
{"type":"avatar.response","seq":1,"timestamp":"...","payload":{"chunk":"好久不见！","is_final":false}}
{"type":"avatar.response","seq":1,"timestamp":"...","payload":{"chunk":"最近过得怎么样？","is_final":true,"emotion":{"joy":0.6,"curiosity":0.8}}}
```

---

## 六、事件消息规范（Pulsar）

### 6.1 消息结构

```json
{
  "event_id": "evt_a1b2c3d4",
  "event_type": "solra.avt.v1.MemoryRecalled",
  "event_version": "1.0",
  "source": {
    "service": "avt-memory-service",
    "instance": "avt-memory-7d8f9-abc12",
    "trace_id": "trace_xyz"
  },
  "timestamp": "2026-06-29T10:30:00.000Z",
  "data": {
    "user_id": "user_123",
    "avatar_id": "avatar_456",
    "memory_id": "mem_789",
    "topic": "海边旅行",
    "recalled_at": "2026-06-29T10:30:00.000Z"
  },
  "metadata": {
    "schema_version": "1",
    "content_type": "application/json"
  }
}
```

### 6.2 事件命名规范

```
格式: solra.{子项目}.v{版本}.{事件名PastTense}

示例:
  solra.avt.v1.MemoryRecalled       # 虚拟人记忆被召回
  solra.spc.v1.SpaceEntered         # 用户进入空间
  solra.spc.v1.SpaceExited          # 用户退出空间
  solra.crt.v1.SpacePublished       # 空间发布
  solra.soc.v1.UserJoinedSpace      # 用户加入多人空间
  solra.soc.v1.GesturePerformed     # 社交信号发出
  solra.grw.v1.AchievementUnlocked  # 成就解锁
  solra.grw.v1.LevelUpgraded        # 等级提升
  solra.grw.v1.DecisiveMomentReached# 决定性时刻触发
  solra.mon.v1.SubscriptionActivated# 订阅激活
  solra.mon.v1.PaymentCompleted     # 支付完成
```

### 6.3 事件订阅契约

```yaml
# 事件消费者声明
subscription:
  name: "grw-achievement-processor"
  topics:
    - "persistent://solra/avt/memory-recalled"
    - "persistent://solra/spc/space-events"
    - "persistent://solra/soc/social-events"
  ack_timeout: 30s
  retry:
    max_attempts: 3
    backoff: exponential  # 1s → 2s → 4s
  dead_letter_topic: "persistent://solra/system/dlq"
```

---

## 七、版本策略

### 7.1 REST API 版本

```
版本方式: URL 路径版本 (v1, v2)

版本生命周期：
  Current (v2) → 全功能，推荐使用
  Deprecated (v1) → 仍可用，响应头 Warning 提示升级，6个月后下线
  Sunset (v0) → 返回 301 Moved Permanently 到新版本

向下兼容规则：
  ✅ 新增字段（老客户端忽略）
  ✅ 新增端点（不影响现有端点）
  ✅ 新增枚举值（老客户端用 default 分支处理）
  ✅ 放宽校验规则
  ❌ 删除/重命名字段
  ❌ 修改字段类型
  ❌ 收紧校验规则
  ❌ 修改分页默认值
```

### 7.2 gRPC 版本

```
- 每个子项目独立版本（avt.v1, avt.v2）
- 服务端同时运行多个版本，通过请求头路由到对应版本
- v1 Deprecated 后保留 12 个月下线
- Breaking Change 通过 buf breaking 在 CI 检测并拦截
```

### 7.3 客户端版本兼容矩阵

| 客户端版本 | 支持 API 版本 | 强制升级 |
|-----------|-------------|---------|
| 最新版本 | v2 (current) + v1 (deprecated) | — |
| 最近 3 个大版本 | v1 | 不强制 |
| 3 个大版本以前 | v1 + 提示升级 | 弹窗提醒 |
| 6 个大版本以前 | 不再支持 | 强制升级 |

---

## 八、API 安全

### 8.1 认证方式

| 客户端类型 | 认证方式 | Token 位置 |
|-----------|---------|-----------|
| 移动端/Web端 | JWT Bearer Token | `Authorization: Bearer {token}` |
| 第三方应用 | OAuth 2.0 Authorization Code + PKCE | `Authorization: Bearer {token}` |
| 服务间调用 | mTLS + Spiffe JWT | gRPC Metadata |
| Webhook | HMAC-SHA256 签名 | `X-Solra-Signature: t=...,v1=...` |

### 8.2 限流策略

| 限流维度 | 策略 | 限制 |
|---------|------|------|
| 全局 | 令牌桶 (APISIX) | 100万 req/s |
| 每用户 | 令牌桶 | 100 req/s |
| 每 IP | 令牌桶 | 500 req/s |
| AI 推理 | 按用户订阅级别 | Free: 20 req/min, Plus: 60, Pro: 300 |
| 空间上传 | 每日配额 | Free: 3个/天, Creator: 30个/天 |
| 写操作 | 每用户每资源 | 10 req/s |

### 8.3 CORS 策略

```
Access-Control-Allow-Origin: https://solra.io (生产) / https://dev.solra.io (开发)
Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Content-Type, Authorization, X-Request-Id, X-Device-Id
Access-Control-Max-Age: 86400
Access-Control-Allow-Credentials: true
```

---

## 九、API 文档自动化

### 9.1 文档生成流水线

```
┌──────────────────────────────────────────────────────────────┐
│                  API 文档自动化流水线                          │
│                                                               │
│  Protobuf 源文件            REST Controller                  │
│       │                          │                           │
│       ▼                          ▼                           │
│  ┌──────────┐            ┌──────────────┐                   │
│  │ buf      │            │ SpringDoc    │                   │
│  │ generate │            │ OpenAPI 3.1  │                   │
│  └────┬─────┘            └──────┬───────┘                   │
│       │                         │                            │
│       └──────────┬──────────────┘                            │
│                  │                                           │
│                  ▼                                           │
│  ┌───────────────────────────────┐                          │
│  │  文档聚合服务                  │                          │
│  │  (合并 gRPC + REST 文档)      │                          │
│  └───────────────┬───────────────┘                          │
│                  │                                           │
│                  ▼                                           │
│  ┌───────────────────────────────┐                          │
│  │  发布到 docs.solra.io/api     │                          │
│  │  · gRPC 文档 (Protobuf Doc)   │                          │
│  │  · REST 文档 (Swagger UI)     │                          │
│  │  · 变更日志 (自动生成)        │                          │
│  └───────────────────────────────┘                          │
└──────────────────────────────────────────────────────────────┘
```

### 9.2 OpenAPI 注解规范

```java
@RestController
@RequestMapping("/v1/spaces")
@Tag(name = "空间消费", description = "空间浏览与消费相关接口")
public class SpaceController {

    @Operation(
        summary = "获取空间详情",
        description = "根据空间ID获取空间的完整信息，包括虚拟人列表和环境配置"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "404", description = "空间不存在",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{spaceId}")
    public ResponseEntity<SpaceDetailResponse> getSpace(
        @Parameter(description = "空间ID", example = "space_1234567890")
        @PathVariable String spaceId) {
        // ...
    }
}
```

---

## 十、附录

### 10.1 API 设计 Checklist

- [ ] URL 使用资源名词复数，不超过 3 级嵌套
- [ ] HTTP 方法语义正确
- [ ] 列表接口已实现分页（页码+游标）
- [ ] 写操作支持幂等键（Idempotency-Key）
- [ ] 请求/响应包含 `request_id` 用于全链路追踪
- [ ] 错误响应包含 machine-readable `code` + human-readable `message`
- [ ] Breaking Change 已通过 buf breaking / OpenAPI diff 检测
- [ ] 新增字段不影响老客户端
- [ ] Protobuf 和 REST 同义 API 的字段名/类型一致
- [ ] 认证/鉴权策略已配置
- [ ] 限流策略已配置
- [ ] OpenAPI / Protobuf 文档已更新

### 10.2 跨子项目 API 依赖矩阵

| 调用方 / 提供方 | AUTH | AVT | SPC | CRT | SOC | GRW | MON | SAF | NOT |
|----------------|------|-----|-----|-----|-----|-----|-----|-----|-----|
| **AUTH** | — | — | — | — | — | — | — | — | — |
| **AVT** | gRPC | — | gRPC | — | — | — | — | gRPC | — |
| **SPC** | gRPC | gRPC | — | gRPC | — | — | — | — | — |
| **CRT** | gRPC | gRPC | gRPC | — | — | — | gRPC | REST | — |
| **SOC** | gRPC | gRPC | gRPC | — | — | — | — | — | Event |
| **GRW** | gRPC | Event | Event | Event | Event | — | Event | — | — |
| **MON** | gRPC | — | — | gRPC | — | gRPC | — | — | — |
| **SAF** | gRPC | gRPC | gRPC | gRPC | gRPC | — | — | — | — |
| **NOT** | gRPC | Event | Event | Event | Event | Event | Event | — | — |

### 10.3 参考文档

- `07索拉应用架构设计.md` — 通信模式与跨子项目集成
- `08索拉技术架构设计.md` — 协议选型与技术实现
- `10索拉数据架构设计.md` — API 对应的数据 Schema
- `11索拉安全架构设计.md` — API 安全策略

---

*创建日期：2026年6月29日*
*文档属性：API 设计规范（REST + gRPC + WebSocket + Event）—— 跨 10 个子项目的统一 API 设计标准*
