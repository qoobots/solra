# Core SDK Toolchain Matrix

四平台 CMake 工具链配置，用于交叉编译 Solra Core SDK。

## 平台矩阵

| 平台 | 架构 | 后端 | 工具链文件 | 编译器 |
|------|------|------|-----------|--------|
| iOS | arm64 | Metal + CoreML | `ios.toolchain.cmake` | Apple Clang |
| Android | arm64-v8a | Vulkan + NNAPI | `android.toolchain.cmake` | NDK Clang |
| Windows | x86_64 | Vulkan | `windows.toolchain.cmake` | MSVC 2022 |
| macOS | arm64+x86_64 | Metal | `macos.toolchain.cmake` | Apple Clang |

> **宏命名约定**：所有工具链统一使用 `SOLRA_ENABLE_*` 宏（如 `SOLRA_ENABLE_METAL`、`SOLRA_ENABLE_VULKAN`），不再使用 `SOLRA_GPU_*`。
> **iOS 部署目标**：统一为 16.0（不再支持 bitcode，Apple 自 Xcode 14 起已弃用）。
> **Android**：不再全局禁用异常和 RTTI，保持与其他平台一致的编译策略。

## 快速开始

```bash
# iOS
cmake -B build/ios -G Xcode \
  -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/ios.toolchain.cmake

# Android (需设置 ANDROID_NDK)
export ANDROID_NDK=$HOME/android-ndk-r26
cmake -B build/android \
  -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a

# Windows
cmake -B build/windows -G "Visual Studio 17 2022" -A x64

# macOS
cmake -B build/macos \
  -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/macos.toolchain.cmake
```
