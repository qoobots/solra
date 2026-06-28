# Core SDK — 跨平台核心引擎

> C++17 · CMake 3.20+ · Conan 依赖管理

## 模块

| 模块 | 说明 |
|------|------|
| `render/` | 渲染引擎（PBR、LOD、阴影） |
| `inference/` | 推理引擎（llama.cpp 封装、NPU 加速） |
| `webrtc/` | WebRTC 封装（RTC、数据通道、音频流） |
| `streaming/` | 流式加载（分块解码、优先级调度） |
| `audio/` | 音频引擎（空间音频） |
| `animation/` | 动画系统（骨骼、混合形状、IK） |
| `storage/` | 本地存储（SQLite 封装、文件缓存） |
| `platform/` | 平台抽象层（iOS/Android/Desktop） |

## 构建

```bash
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --parallel
```

## 测试

```bash
cd build
ctest --output-on-failure
```
