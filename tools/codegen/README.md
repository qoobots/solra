# Solra 代码生成器 (codegen)

Proto → 多语言代码一键生成工具，支持6种目标语言。

## 使用

```bash
# 全量生成
python tools/codegen/codegen.py

# 指定语言
python tools/codegen/codegen.py -l java python typescript

# 指定协议域
python tools/codegen/codegen.py -d common avt spc -l java kotlin

# Makefile 入口
make -f tools/codegen/Makefile all
make -f tools/codegen/Makefile java
```

## 支持的语言

| 语言 | 目标 | 框架 |
|------|------|------|
| Java | services/ | Spring Boot gRPC |
| Python | ai-services/ | gRPC |
| TypeScript | clients/web/ | gRPC-Web |
| C++ | core/ | gRPC C++ |
| Kotlin | clients/android/ | gRPC Kotlin |
| Swift | clients/ios/ | gRPC Swift |

## 协议域

| 优先级 | 协议域 |
|--------|--------|
| P0 | common, avt, spc, auth, saf |
| P1 | crt, soc, grw, not |
| P2 | mon |

## 依赖

- `buf` (推荐) — `brew install bufbuild/buf/buf` 或直接下载
- `protoc` (fallback) — `brew install protobuf`

## 输出

生成代码输出到 `generated/{lang}/` 目录。
