# 基础设施层（Infra）API设计

> infra/ 的"API"——Terraform模块接口、Helm Values、ArgoCD App配置

---

## 一、Terraform 模块接口

```hcl
# 模块输入
module "k8s_cluster" {
  source = "./modules/kubernetes"

  cluster_name    = "solra-prod"
  node_count      = 5
  machine_type    = "c3.2xlarge"
  gpu_node_count  = 2
  gpu_machine_type = "g5.2xlarge"
}

# 模块输出
output "kubeconfig" { ... }
output "cluster_endpoint" { ... }
```

---

## 二、Helm Values 规范

```yaml
# values.yaml
replicaCount: 2

image:
  repository: solra/auth-service
  tag: latest

resources:
  requests: {cpu: 250m, memory: 256Mi}
  limits: {cpu: 500m, memory: 512Mi}

env:
  - name: DB_HOST
    valueFrom:
      secretKeyRef:
        name: auth-db
        key: host
```

---

## 三、ArgoCD Application

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
spec:
  source:
    repoURL: https://github.com/solra/solra
    path: infra/kubernetes/charts/auth-service
  destination:
    namespace: solra
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```
