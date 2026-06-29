# Solra Infrastructure

## 组件

| 工具 | 路径 | 用途 |
|------|------|------|
| Docker Compose | docker/ | 本地开发基础设施 |
| Helm Charts | kubernetes/helm/ | 生产级 K8s 部署 |
| ArgoCD | kubernetes/argocd/ | GitOps 自动同步 |
| APISIX | apisix/ | API 网关路由配置 |
| Terraform | terraform/ | 多云资源编排 |
| 部署脚本 | deploy-dev.ps1 / deploy-dev.sh | 一键开发环境 |
| 监控 | monitoring/ | Prometheus + Grafana + Jaeger |

## 快速开始

```bash
# Windows
.\infra\deploy-dev.ps1

# Linux/macOS
bash infra/deploy-dev.sh
```

## 环境矩阵

| 环境 | Terraform | ArgoCD | 用途 |
|------|-----------|--------|------|
| dev | ✅ | 自动同步 | 开发测试 |
| staging | ✅ | 自动同步 | 预发布验证 |
| prod | ✅ | 手动触发 | 生产环境 |

## 资源规格 (dev)

| 对象 | 规格 | 数量 |
|------|------|------|
| K8s Worker | 4C16G (ecs.g7.xlarge) | 2 |
| PostgreSQL | pg.n4.2c.2m / 20GB | 1 |
| Redis | 1GB | 1 |
| Kafka | 3节点 / 50GB | 3 |
