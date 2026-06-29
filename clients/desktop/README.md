# Solra Desktop Client

基于 Tauri 2 + Rust + Vue 3 + Core SDK FFI 的 Windows 桌面客户端 —— 索拉主力 3D 社交体验平台。

## 架构

```
Vue 3 UI (WebView)  →  Tauri IPC  →  Rust FFI  →  Core C ABI  →  C++17 引擎
     ↑                      ↑              ↑              ↑
  管理界面              命令桥接       语言边界      3D渲染/推理/WebRTC/流式
```

## 技术栈

- **壳层**: Tauri 2 (Rust) — 轻量级桌面框架
- **UI**: Vue 3.4 + TypeScript + Vite + Element Plus (100% 复用 web/ 架构)
- **3D 渲染**: OpenGL/Vulkan (通过 Core SDK FFI)
- **端侧推理**: llama.cpp / ONNX Runtime (通过 Core SDK FFI)
- **WebRTC**: libwebrtc (通过 Core SDK FFI)
- **流式加载**: QUIC + 分块下载 (通过 Core SDK FFI)

## 快速开始

```bash
# 前置条件
# 1. 安装 Rust: https://rustup.rs/
# 2. 编译 Core SDK: cd ../../core && cmake --build build/windows --target solra_core
# 3. 复制 libsolracore.dll 到 core/ 目录

cd clients/desktop
npm install
npm run tauri dev     # 开发模式
npm run tauri build   # 生产构建
```

## 目录结构

```
clients/desktop/
├── src-tauri/                   # Tauri 2 Rust 后端
│   └── src/
│       ├── main.rs              # 入口
│       ├── lib.rs               # 库入口
│       ├── core/                # Core SDK FFI 桥接
│       │   ├── ffi.rs           #   C ABI 声明
│       │   ├── render.rs        #   渲染桥接
│       │   ├── inference.rs     #   推理桥接
│       │   ├── streaming.rs     #   流式加载桥接
│       │   └── webrtc.rs        #   WebRTC 桥接
│       ├── ipc/                 # Tauri IPC 命令
│       │   ├── space_cmd.rs     #   空间命令
│       │   ├── avatar_cmd.rs    #   虚拟人命令
│       │   ├── render_cmd.rs    #   渲染控制命令
│       │   └── system_cmd.rs    #   系统命令
│       └── services/            # Rust 端业务服务
│           ├── auth_service.rs  #   认证
│           ├── api_client.rs    #   HTTP/gRPC 客户端
│           └── cache_service.rs #   本地缓存
├── src/                         # Vue 3 前端（复用 web/ 架构）
│   ├── views/                   # 页面组件
│   ├── components/renderer/     # 3D 渲染视口
│   ├── api/                     # API + Tauri IPC
│   └── stores/                  # Pinia 状态
├── core/                        # Core SDK 库文件放置目录
├── package.json
├── Cargo.toml
└── tauri.conf.json
```

## 与 Web 客户端的关系

Desktop 端 UI 层 100% 复用 Web 端的 Vue 3 组件架构（路由/页面/状态管理/样式），仅将 3D 渲染能力从 WebGL 替换为通过 Tauri IPC 调用的 Core SDK 原生渲染管线。

Web 端定位为轻量访问端（空间浏览、用户管理），Desktop 端定位为全功能 3D 体验平台。
