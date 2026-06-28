# 索拉（Solra）工程

> Monorepo 统一仓库 — 10 个子项目 × 4 个平台的统一工程
>
> 下一代去中心化自主虚拟世界

## 快速开始

```bash
make dev    # 启动本地开发环境
make proto  # 生成 Proto 代码
make build  # 全量构建
make test   # 全量测试
```

## 项目结构

| 目录 | 说明 | 技术栈 |
|------|------|--------|
| `contracts/` | 协议定义（单一事实源） | Protobuf / Avro / OpenAPI |
| `core/` | 跨平台核心引擎 | C++17 / CMake |
| `services/` | 业务微服务群 | Java 17 / Spring Boot 3.x |
| `ai-services/` | AI 推理服务群 | Python 3.11 / FastAPI |
| `clients/` | 客户端应用 | Swift / Kotlin / TypeScript |
| `infra/` | 基础设施即代码 | Terraform / K8s / Docker |
| `tools/` | 开发工具 & 脚本 | Shell / Python |
| `01docs/` | 全项目文档 | Markdown |

## 子项目

| 缩写 | 中文名 | 领域类型 |
|------|--------|---------|
| AVT | 虚拟人交互 | 核心域 |
| SPC | 空间消费 | 核心域 |
| CRT | 空间创作 | 支撑域 |
| SOC | 社交在场 | 支撑域 |
| GRW | 用户成长 | 支撑域 |
| MON | 商业化 | 支撑域 |
| AUTH | 身份认证 | 通用域 |
| SAF | 内容安全 | 通用域 |
| NOT | 通知消息 | 通用域 |
| INF | 基础设施 | 通用域 |

## 许可证

[Apache License 2.0](LICENSE)
