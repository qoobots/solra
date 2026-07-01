# 11 API 设计 — Solra 桌面客户端

> **文档状态**：初稿  
> **最后更新**：2026-06-30  
> **适用版本**：Solra Desktop v0.1.0

---

## 1. 通信架构

```
┌──────────────────────┐       ┌──────────────────┐
│   Vue 3 前端          │       │   云端服务         │
│                      │ HTTP  │                  │
│  axios ──────────────┼──────→│  REST API        │
│                      │  WSS  │  (APISIX)        │
│  WebSocket ──────────┼──────→│  WebSocket       │
│                      │       │  gRPC            │
├──────────────────────┤       └──────────────────┘
│  Tauri IPC (内部)     │
│                      │
│  invoke() ──────────→│
│  listen() ←──────────│
│                      │
├──────────────────────┤
│  Rust 后端            │
│  ┌────────────────┐  │
│  │  IPC Handlers   │  │
│  │  (render_cmd,   │  │
│  │   space_cmd,    │  │
│  │   avatar_cmd,   │  │
│  │   system_cmd)   │  │
│  └───────┬────────┘  │
│          │ FFI        │
│  ┌───────▼────────┐  │
│  │  Core SDK       │  │
│  │  (C++17)        │  │
│  └────────────────┘  │
└──────────────────────┘
```

---

## 2. Tauri IPC 接口

### 2.1 渲染命令（render_cmd）

#### `init_renderer`
初始化渲染引擎。

```typescript
// 请求
interface InitRendererRequest {
  width: number;     // 视口宽度 (px)
  height: number;    // 视口高度 (px)
}

// 响应
interface RenderInfo {
  initialized: boolean;
  gpu_backend: string;    // "Vulkan" | "OpenGL" | "Metal" | "Software Rasterizer"
  resolution: {
    width: number;
    height: number;
  };
}
```

```rust
#[tauri::command]
async fn init_renderer(
    width: u32,
    height: u32,
    state: State<'_, AppState>
) -> Result<RenderInfo, String>
```

#### `render_frame`
执行一帧渲染。

```typescript
// 响应
interface FrameResult {
  fps: number;
  frame_time_ms: number;
}
```

```rust
#[tauri::command]
async fn render_frame(
    state: State<'_, AppState>
) -> Result<FrameResult, String>
```

#### `get_gpu_info`
获取 GPU 信息。

```typescript
// 响应
interface GpuInfo {
  backend: string;
  vendor: string;
  renderer: string;
  version: string;
}
```

#### `get_fps`
获取当前帧率统计。

```typescript
// 响应
interface FpsStats {
  current: number;
  average: number;
  min: number;
  max: number;
  frame_count: number;
}
```

#### `load_scene`
加载 3D 场景。

```typescript
// 请求
interface LoadSceneRequest {
  scene_id: string;
  scene_data?: object;    // 可选：直接传入场景 JSON
}

// 响应
interface SceneInfo {
  scene_id: string;
  node_count: number;
  root_node_id: string;
}
```

#### `get_root_node` / `get_child_count` / `get_child_id`
场景节点查询。

```typescript
interface SceneNode {
  id: string;
  name: string;
  node_type: string;
  visible: boolean;
  transform: Float32Array;  // 4x4 矩阵
}
```

#### `destroy_renderer`
释放渲染资源。

```rust
#[tauri::command]
async fn destroy_renderer(
    state: State<'_, AppState>
) -> Result<(), String>
```

### 2.2 空间命令（space_cmd）

#### `list_spaces`
获取空间列表。

```typescript
// 请求
interface ListSpacesRequest {
  page?: number;
  page_size?: number;
  filter?: string;
  sort?: 'hot' | 'new' | 'popular';
}

// 响应
interface SpaceListResult {
  spaces: SpaceSummary[];
  total: number;
  page: number;
}

interface SpaceSummary {
  id: string;
  name: string;
  description: string;
  thumbnail_url: string;
  tags: string[];
  online_count: number;
  rating: number;
}
```

#### `get_space_detail`
获取空间详情。

```typescript
interface SpaceDetail {
  id: string;
  name: string;
  description: string;
  creator: UserSummary;
  scene_data_url: string;
  online_users: UserSummary[];
  created_at: string;
  updated_at: string;
}
```

### 2.3 虚拟人命令（avatar_cmd）

#### `get_avatar_info`
获取虚拟人信息。

```typescript
interface AvatarInfo {
  id: string;
  name: string;
  model_url: string;
  personality: string;
  animations: string[];
}
```

#### `send_avatar_message`
向虚拟人发送消息。

```typescript
// 请求
interface AvatarMessageRequest {
  avatar_id: string;
  message: string;
  context?: MessageRecord[];
}

// 响应（流式）
interface AvatarMessageChunk {
  token: string;
  is_end: boolean;
}
```

### 2.4 系统命令（system_cmd）

#### `get_system_info`
获取系统信息。

```typescript
interface SystemInfo {
  os: string;
  os_version: string;
  cpu_cores: number;
  total_memory_gb: number;
  gpu_devices: string[];
}
```

#### `open_url`
在默认浏览器打开 URL。

```rust
#[tauri::command]
async fn open_url(url: String) -> Result<(), String>
```

---

## 3. HTTP API

### 3.1 API 基础配置

```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'https://api.solra.io';
const API_VERSION = 'v1';

// axios 实例
const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/${API_VERSION}`,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});
```

### 3.2 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录 |
| POST | `/auth/register` | 注册 |
| POST | `/auth/refresh` | 刷新 Token |
| POST | `/auth/logout` | 退出登录 |
| GET | `/auth/me` | 获取当前用户信息 |

### 3.3 空间接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/spaces` | 空间列表 |
| GET | `/spaces/:id` | 空间详情 |
| GET | `/spaces/:id/scene` | 场景数据 |
| POST | `/spaces` | 创建空间（创作者） |
| PUT | `/spaces/:id` | 更新空间 |
| DELETE | `/spaces/:id` | 删除空间 |

### 3.4 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/users/:id` | 用户信息 |
| PUT | `/users/me` | 更新个人信息 |
| GET | `/users/me/friends` | 好友列表 |
| POST | `/users/me/friends` | 添加好友 |

### 3.5 消息接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/conversations` | 会话列表 |
| GET | `/conversations/:id/messages` | 消息历史 |
| POST | `/conversations/:id/messages` | 发送消息 |

---

## 4. WebSocket

### 4.1 连接

```
WSS: wss://api.solra.io/v1/ws?token={JWT}
```

### 4.2 事件

| 事件 | 方向 | 说明 |
|------|------|------|
| `space:user_join` | Server → Client | 用户进入空间 |
| `space:user_leave` | Server → Client | 用户离开空间 |
| `space:user_move` | Server → Client | 用户位置更新 |
| `message:new` | Server → Client | 新消息 |
| `avatar:typing` | Server → Client | 虚拟人正在输入 |
| `notification` | Server → Client | 系统通知 |
| `ping` | Client → Server | 心跳 |
| `pong` | Server → Client | 心跳响应 |

### 4.3 心跳

```
间隔：30 秒
超时：90 秒无响应 → 断开重连
重连策略：指数退避（1s → 2s → 4s → 8s → 16s → 30s max）
```

---

## 5. 错误码规范

### 5.1 IPC 错误

| 错误码 | 说明 |
|--------|------|
| `CORE_SDK_NOT_FOUND` | Core SDK DLL 未找到 |
| `CORE_SDK_LOAD_ERROR` | DLL 加载失败 |
| `RENDER_NOT_INITIALIZED` | 渲染引擎未初始化 |
| `SCENE_NOT_FOUND` | 场景不存在 |
| `INVALID_CONFIG` | 无效的渲染配置 |
| `IPC_TIMEOUT` | IPC 调用超时 |

### 5.2 HTTP 错误

| 状态码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未认证 / Token 过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 429 | 请求频率限制 |
| 500 | 服务器内部错误 |
| 503 | 服务暂不可用 |

### 5.3 错误响应格式

```typescript
interface ApiError {
  code: string;           // 错误码
  message: string;        // 人类可读错误信息
  details?: object;       // 可选的详细信息
  request_id?: string;    // 请求追踪 ID
}
```

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 | 变更人 |
|------|------|---------|--------|
| 2026-06-30 | v0.1 | 初稿创建 | - |
