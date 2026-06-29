# Solra OpenAPI 规范

## 文件说明

| 文件 | 用途 | 受众 | 版本 |
|------|------|------|------|
| `service-api.yaml` | 微服务间 REST API 规范 | 后端团队 | 1.0.0 |
| `client-api.yaml` | 客户端 BFF 层 API 规范 | 前端/移动端团队 | 1.0.0 |

## 设计原则

1. **契约先行**：API 变更需先更新 OpenAPI spec，再实现代码
2. **服务间 JWT**：所有 service-to-service 调用使用 ServiceJWT 认证
3. **BFF 模式**：客户端不直接调用微服务，通过 API Gateway/BFF 聚合
4. **SSE 流式**：Avatar 对话等长连接场景优先使用 Server-Sent Events

## 工具链

```bash
# 校验
npm install -g @redocly/cli
redocly lint service-api.yaml
redocly lint client-api.yaml

# 生成文档
redocly build-docs service-api.yaml -o docs/service-api.html
redocly build-docs client-api.yaml -o docs/client-api.html

# 代码生成
openapi-generator generate -i service-api.yaml -g spring -o ../gen/service-client
openapi-generator generate -i client-api.yaml -g typescript-fetch -o ../gen/client-sdk
```

## 版本策略

- 使用语义化版本 (MAJOR.MINOR.PATCH)
- Breaking changes 触发 MAJOR 版本升级
- 新增端点触发 MINOR 版本升级
- 文档修正触发 PATCH 版本升级
