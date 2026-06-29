# Solra Terraform 多云资源编排

## 支持的云平台

| 平台 | Provider | 用途 |
|------|----------|------|
| 阿里云 | alicloud | 主生产集群 (ACK/RDS/Redis/Kafka/OSS) |
| AWS | aws | 海外区域部署 |
| 腾讯云 | tencentcloud | 小程序生态 (可选) |

## 模块结构

```
terraform/
├── main.tf              # 根模块
├── variables.tf          # 变量定义
├── outputs.tf            # 输出
├── modules/
│   ├── network/          # VPC/交换机/安全组
│   ├── kubernetes/       # K8s 集群 (ACK/EKS/TKE)
│   ├── database/         # RDS PostgreSQL
│   ├── cache/            # Redis
│   ├── messaging/        # Kafka
│   ├── storage/          # OSS/S3/COS
│   └── cdn/              # CDN + DNS
├── environments/
│   ├── dev.tfvars        # 开发环境
│   ├── staging.tfvars    # 预发布
│   └── prod.tfvars       # 生产
└── README.md
```

## 使用

```bash
# 初始化
terraform init

# 计划
terraform plan -var-file=environments/dev.tfvars

# 部署
terraform apply -var-file=environments/dev.tfvars
```
