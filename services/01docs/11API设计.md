# 微服务层（Services）API设计

> services/ gRPC与REST API设计规范——接口命名、请求/响应格式、错误码体系

---

## 一、gRPC API

### 1.1 标准RPC方法

```protobuf
service AvtService {
  rpc SendMessage(SendMessageRequest) returns (SendMessageResponse);
  rpc StreamResponse(StreamResponseRequest) returns (stream DialogueTurn);
  rpc GetConversationHistory(GetConversationHistoryRequest) returns (GetConversationHistoryResponse);
  rpc ListConversations(ListConversationsRequest) returns (ListConversationsResponse);
}
```

### 1.2 命名约定

| 操作 | 方法名 | 说明 |
|------|--------|------|
| 查询单个 | `Get{Resource}` | 返回单个实体 |
| 查询列表 | `List{Resource}s` | 支持分页 |
| 创建 | `Create{Resource}` | 返回创建后的实体 |
| 更新 | `Update{Resource}` | 部分/全量更新 |
| 删除 | `Delete{Resource}` | 逻辑删除 |
| 自定义操作 | `{Verb}{Resource}` | SendMessage/GeneratePreview |

---

## 二、REST API (BFF/外部)

### 2.1 路径规范

```
GET    /api/v1/spaces              # 列表
GET    /api/v1/spaces/{id}         # 详情
POST   /api/v1/spaces/{id}/enter   # 进入空间
GET    /api/v1/spaces/{id}/preview # 预览卡片
```

### 2.2 统一响应

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "trace_id": "a1b2c3d4"
}
```

---

## 三、错误码

| 范围 | 含义 |
|------|------|
| 0 | 成功 |
| 40000-40099 | 参数错误 |
| 40100-40199 | 未认证 |
| 40300-40399 | 无权限 |
| 40400-40499 | 资源不存在 |
| 42900-42999 | 限流 |
| 50000-50099 | 服务内部错误 |
