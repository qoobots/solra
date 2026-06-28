# 协议定义层（Contracts）API设计

> contracts/ 的API设计——Proto RPC定义规范、gRPC服务设计、OpenAPI规范

---

## 一、gRPC 服务定义模式

### 1.1 标准服务模板

```protobuf
syntax = "proto3";

package solra.avt.v1;

import "solra/common/v1/common.proto";

// AvtService 虚拟人交互服务
service AvtService {
  // 发送消息给虚拟人
  rpc SendMessage(SendMessageRequest) returns (SendMessageResponse);
  
  // 流式接收虚拟人回复
  rpc StreamResponse(StreamResponseRequest) returns (stream DialogueTurn);
  
  // 获取对话历史
  rpc GetConversationHistory(GetConversationHistoryRequest) returns (GetConversationHistoryResponse);
}

message SendMessageRequest {
  solra.common.v1.UserId user_id = 1;
  solra.common.v1.SpaceId space_id = 2;
  string content = 3; // 用户输入文本
}

message SendMessageResponse {
  string conversation_id = 1;
  DialogueTurn turn = 2;
}
```

### 1.2 RPC 命名约定

| 模式 | 示例 | 说明 |
|------|------|------|
| Get{Resource} | `GetSpace` | 获取单个资源 |
| List{Resource}s | `ListSpaces` | 列表查询 |
| Create{Resource} | `CreateSpace` | 创建资源 |
| Update{Resource} | `UpdateSpace` | 更新资源 |
| Delete{Resource} | `DeleteSpace` | 删除资源 |
| {Action}{Resource} | `SendMessage` | 自定义操作 |

---

## 二、API 版本管理

### 2.1 Proto包版本

```
solra.avt.v1     → 稳定版本
solra.avt.v2alpha1 → 预览版本
```

### 2.2 API演进策略

| 阶段 | 做法 |
|------|------|
| 新增RPC | 直接在v1 Service中添加 |
| 废弃RPC | 标记`option deprecated = true;` |
| 破坏性变更 | 新建v2 Service，保留v1时期 |

---

## 三、OpenAPI 规范位置

| API | 路径 | 消费者 |
|-----|------|--------|
| auth-service REST API | `openapi/services/auth.yaml` | 客户端直接调用 |
| BFF层聚合API | `openapi/client/bff.yaml` | iOS/Android/Web |
