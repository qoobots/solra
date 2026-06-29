# APISIX API 网关

## 路由架构

```
                  ┌─────────────────────────────────┐
                  │        APISIX Gateway           │
                  │                                 │
   Client ──────▶ │  /v1/*     → BFF (api-gateway)  │
                  │  /internal/* → 微服务群 (JWT)    │
                  │  /ai/*       → AI 服务群         │
                  └─────────────────────────────────┘
```

## 路由清单

| 路由 | 目标 | 认证 | 限流 | 超时 |
|------|------|------|------|------|
| `/v1/*` | BFF API Gateway | Client JWT | 100 req/s | — |
| `/internal/auth/*` | auth-service | Service JWT | — | — |
| `/internal/safety/*` | saf-service | Service JWT | — | — |
| `/internal/social/*` | soc-service | Service JWT | — | — |
| `/internal/growth/*` | grw-service | Service JWT | — | — |
| `/internal/notifications/*` | not-service | Service JWT | — | — |
| `/internal/spaces/*` | spc-service | Service JWT | — | — |
| `/internal/mon/*` | mon-service | Service JWT | — | — |
| `/ai/llm/*` | llm-router | — | — | 120s |
| `/ai/tts/*` | tts-service | — | — | 60s |

## 部署

```bash
helm repo add apisix https://charts.apiseven.com
helm upgrade --install apisix apisix/apisix \
  --set apisix.ssl.enabled=true \
  --set apisix.admin.allow.ipList="{0.0.0.0/0}" \
  -f routes.yaml \
  --namespace apisix --create-namespace
```
