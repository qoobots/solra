# Solra ArgoCD GitOps 配置

## 环境矩阵

| 环境 | ArgoCD App | 自动同步 | selfHeal | 触发 |
|------|-----------|---------|----------|------|
| dev | `solra-dev` | ✅ | ✅ | main 分支推送 |
| staging | `solra-staging` | ✅ | ❌ | main 分支推送 |
| prod | `solra-prod` | ❌ 手动 | ❌ | 审批后手动触发 |

## 部署流程

```
Git Push → CI Build → Docker Push → ArgoCD Sync → K8s Apply
```

## 初始化

```bash
# 安装 ArgoCD 到集群
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 注册 Git 仓库
argocd repo add https://github.com/solra-io/solra.git

# 创建 App
kubectl apply -f infra/kubernetes/argocd/
```
