# 客户端层（Clients）API设计

> 客户端与后端通信——gRPC/REST/SSE协议

---

## 一、通信协议选择

| 场景 | 协议 | 说明 |
|------|------|------|
| 查询/操作 | gRPC (Protobuf) | 服务间标准通信 |
| 文件上传 | REST (multipart) | 图片/资产上传 |
| AI对话流 | SSE (Server-Sent Events) | Token流式推送 |
| 实时同步 | WebSocket/WebRTC | 多人同步 |
| CDN下载 | HTTP/3 | 3D资产加载 |

---

## 二、iOS API Client

```swift
// gRPC Client
let client = AvtServiceAsyncClient(channel: channel)
let response = try await client.sendMessage(request)

// SSE Stream
let stream = apiClient.chatStream(text: "你好")
for try await token in stream {
    // 更新UI
}
```

---

## 三、Android API Client

```kotlin
// gRPC Client
val stub = AvtServiceGrpcKt.AvtServiceCoroutineStub(channel)
val response = stub.sendMessage(request)

// SSE Stream
apiClient.chatStream("你好").collect { token ->
    // 更新UI
}
```

---

## 四、统一错误处理

```swift
enum ClientError: Error {
    case network(underlying: Error)
    case server(code: Int, message: String)
    case auth(expired: Bool)
    case parsing(underlying: Error)
}
```
