# Solra 服务 Helm Chart

## 安装

```bash
# 添加依赖 subcharts
helm dependency update

# 开发环境
helm upgrade --install solra-dev . \
  -f values-dev.yaml \
  --namespace solra --create-namespace

# 生产环境
helm upgrade --install solra-prod . \
  -f values-prod.yaml \
  --namespace solra --create-namespace
```

## 包含内容

- 10 个 Java 微服务 (auth/avt/spc/soc/crt/grw/not/saf/mon)
- 6 个 Python AI 服务 (llm-router/embedding/tts/recommendation/safety/gpu-scheduler)
- 健康检查 (Liveness/Readiness Probes)
- Prometheus 指标采集注解
- 合理资源限制

## 部署顺序

1. 基础设施: PostgreSQL / Redis / Kafka
2. 核心服务: auth-service → saf-service
3. 业务服务: spc/soc/crt/grw/not/mon
4. AI 服务: gpu-scheduler → llm-router → embedding/tts/safety
5. API 网关: APISIX 路由更新
