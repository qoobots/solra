# 19_统一API接口文档 (OpenAPI/Swagger)

> **文档编号**：03-19
> **版本号**：v1.0
> **编制人**：接口组
> **审核人**：技术委员会
> **状态**：已发布
> **更新日期**：2026-06-30

---

## 1. API设计规范

### 1.1 URL规范

```
格式: /api/v{version}/{domain}/{resource}[/{id}][/{sub-resource}]

示例:
GET    /api/v1/auth/users/me                    # 获取当前用户
POST   /api/v1/auth/users                       # 注册
GET    /api/v1/space/spaces                     # 空间列表
GET    /api/v1/space/spaces/{spaceId}           # 空间详情
POST   /api/v1/space/spaces/{spaceId}/enter     # 进入空间
GET    /api/v1/avatar/avatars/{avatarId}        # 虚拟人详情
POST   /api/v1/avatar/conversations/{id}/messages  # 发送消息
```

### 1.2 HTTP方法

| 方法 | 用途 | 幂等 |
|------|------|------|
| GET | 查询 | ✅ |
| POST | 创建/操作 | ❌ |
| PUT | 全量更新 | ✅ |
| PATCH | 部分更新 | ❌ |
| DELETE | 删除 | ✅ |

### 1.3 请求头

| Header | 说明 |
|--------|------|
| `Authorization: Bearer {token}` | 认证Token |
| `Content-Type: application/json` | 请求体类型 |
| `Accept: application/json` | 期望响应类型 |
| `X-Request-ID: {uuid}` | 请求追踪ID |
| `X-Client-Version: {version}` | 客户端版本 |
| `Accept-Language: zh-CN` | 语言偏好 |

---

## 2. 通用响应格式

### 2.1 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "requestId": "uuid",
  "timestamp": "2026-06-30T12:00:00Z"
}
```

### 2.2 分页响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [ ... ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 150,
      "totalPages": 8,
      "hasNext": true
    }
  }
}
```

### 2.3 错误响应

```json
{
  "code": 401001,
  "message": "认证失败：Token已过期",
  "details": [
    {
      "field": "token",
      "reason": "expired",
      "message": "Token已于2026-06-30T11:45:00Z过期"
    }
  ],
  "requestId": "uuid",
  "timestamp": "2026-06-30T12:00:00Z"
}
```

---

## 3. AUTH认证API

### 3.1 POST /api/v1/auth/users/register

注册新用户。

**Request:**
```json
{
  "phone": "13800138000",
  "email": "user@example.com",
  "password": "SecureP@ss123",
  "verificationCode": "123456",
  "nickname": "用户名"
}
```

**Response (201):**
```json
{
  "code": 0,
  "data": {
    "userId": "uuid",
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "expiresIn": 900,
    "user": {
      "id": "uuid",
      "nickname": "用户名",
      "avatarUrl": "https://..."
    }
  }
}
```

### 3.2 POST /api/v1/auth/users/login

用户登录。

**Request:**
```json
{
  "account": "13800138000",
  "password": "SecureP@ss123",
  "deviceInfo": {
    "deviceId": "device_hash",
    "deviceType": "ios",
    "osVersion": "18.0"
  }
}
```

### 3.3 POST /api/v1/auth/token/refresh

刷新Token。

**Request:**
```json
{
  "refreshToken": "eyJhbG..."
}
```

### 3.4 POST /api/v1/auth/oauth/{provider}

第三方登录（provider: wechat/apple/google）。

**Request:**
```json
{
  "code": "authorization_code",
  "state": "csrf_state",
  "deviceInfo": { ... }
}
```

---

## 4. AVT虚拟人API

### 4.1 POST /api/v1/avatar/avatars

创建虚拟人。

**Request:**
```json
{
  "name": "我的虚拟人",
  "style": "realistic",
  "gender": "female",
  "faceParams": {
    "eyes": { "size": 1.0, "spacing": 0.0 },
    "nose": { "length": 0.0, "width": 0.0 },
    "mouth": { "width": 0.0, "thickness": 0.0 }
  },
  "bodyParams": {
    "height": 170,
    "build": "slim"
  },
  "photoUrl": "https://... (可选，AI生成)"
}
```

### 4.2 POST /api/v1/avatar/conversations/{conversationId}/messages

发送消息（SSE流式返回）。

**Request:**
```json
{
  "content": "今天心情怎么样？",
  "contentType": "text"
}
```

**Response (SSE Stream):**
```
event: token
data: {"token": "今天", "index": 0}

event: token
data: {"token": "心情", "index": 1}

event: token
data: {"token": "不错", "index": 2}

...

event: complete
data: {"messageId": "uuid", "emotion": "happy", "tokensUsed": 45, "latencyMs": 320}
```

### 4.3 GET /api/v1/avatar/avatars

获取虚拟人列表。

**Query:**
```
?page=1&size=20&style=realistic&status=ACTIVE
```

### 4.4 PUT /api/v1/avatar/avatars/{avatarId}/style

切换虚拟人风格。

**Request:**
```json
{
  "style": "anime"
}
```

---

## 5. SPC空间API

### 5.1 GET /api/v1/space/spaces/feed

推荐流。

**Query:**
```
?page=1&size=10&category=social
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "items": [
      {
        "id": "uuid",
        "name": "星空图书馆",
        "description": "在星空下阅读与交流...",
        "coverUrl": "https://...",
        "creator": {
          "id": "uuid",
          "nickname": "创作者",
          "avatarUrl": "https://..."
        },
        "currentUsers": 42,
        "maxCapacity": 100,
        "rating": 4.5,
        "tags": ["阅读", "安静", "星空"],
        "accessType": "public",
        "entranceFee": 0
      }
    ],
    "pagination": { ... }
  }
}
```

### 5.2 GET /api/v1/space/spaces/search

空间搜索。

**Query:**
```
?q=图书馆&category=social&sort=hot&page=1&size=20
```

### 5.3 POST /api/v1/space/spaces/{spaceId}/enter

进入空间。

**Response:**
```json
{
  "code": 0,
  "data": {
    "instanceId": "uuid",
    "wsEndpoint": "wss://realtime.solra.io/ws?instance=uuid&token=xxx",
    "rtcConfig": {
      "iceServers": [{ "urls": ["stun:stun.solra.io:3478"] }]
    },
    "spaceState": {
      "objects": [...],
      "users": [...],
      "lighting": {...}
    }
  }
}
```

### 5.4 POST /api/v1/space/spaces/{spaceId}/leave

离开空间。

---

## 6. SOC社交API

### 6.1 POST /api/v1/social/friends/requests

发送好友申请。

**Request:**
```json
{
  "userId": "target_user_uuid",
  "message": "你好，在星空图书馆看到你"
}
```

### 6.2 GET /api/v1/social/friends

好友列表。

**Query:**
```
?status=ACCEPTED&page=1&size=50
```

### 6.3 POST /api/v1/social/messages

发送私聊消息。

**Request:**
```json
{
  "receiverId": "uuid",
  "content": "你好！",
  "contentType": "text"
}
```

---

## 7. MON商城API

### 7.1 GET /api/v1/store/products

商品列表。

**Query:**
```
?category=outfit&sort=newest&page=1&size=20
```

### 7.2 POST /api/v1/store/orders

创建订单。

**Request:**
```json
{
  "productId": "uuid",
  "quantity": 1,
  "paymentMethod": "wechat"
}
```

**Response:**
```json
{
  "code": 0,
  "data": {
    "orderNo": "SO20260630120000001",
    "amount": 9900,
    "currency": "SLC",
    "paymentUrl": "weixin://...",
    "expiresAt": "2026-06-30T12:15:00Z"
  }
}
```

---

## 8. WebSocket协议

### 8.1 连接

```
wss://realtime.solra.io/ws?instance={instanceId}&token={jwt}
```

### 8.2 消息格式

```json
{
  "type": "message_type",
  "payload": { ... },
  "timestamp": "2026-06-30T12:00:00.000Z",
  "senderId": "uuid"
}
```

### 8.3 消息类型

| Type | 方向 | 说明 |
|------|------|------|
| `position_update` | C→S→C | 位置同步(20Hz) |
| `chat_message` | C→S→C | 聊天消息 |
| `voice_start` | C→S | 开始语音 |
| `voice_data` | C→S→C | 语音数据 |
| `voice_end` | C→S | 结束语音 |
| `emote` | C→S→C | 表情动作 |
| `object_interact` | C→S→C | 物体交互 |
| `user_join` | S→C | 用户进入 |
| `user_leave` | S→C | 用户离开 |
| `space_update` | S→C | 空间状态更新 |
| `heartbeat` | C→S | 心跳(30s) |

---

## 9. API版本管理

| 策略 | 说明 |
|------|------|
| URL版本 | `/api/v1/`, `/api/v2/` |
| 废弃通知 | `Deprecation: true` Header + `Sunset` Header |
| 废弃周期 | 至少保留2个版本，旧版本6个月后移除 |
| 兼容性 | 新增字段向后兼容，不删除已有字段 |

---

> **注**：完整OpenAPI 3.1规范文件存放于 `contracts/api/openapi/` 目录。gRPC Proto文件存放于 `contracts/proto/`。
