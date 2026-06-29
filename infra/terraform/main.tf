# Terraform 多云资源编排
# 支持 AWS / GCP / 阿里云

terraform {
  required_version = ">= 1.8.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.28"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.13"
    }
  }
  backend "s3" {
    # TODO: 配置远程 backend（AWS S3 / GCP GCS / 阿里云 OSS）
    bucket = "solra-terraform-state"
    key    = "dev/terraform.tfstate"
    region = "ap-southeast-1"
  }
}

provider "kubernetes" {
  config_path = "~/.kube/config"
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}

# ── 命名空间 ──
resource "kubernetes_namespace" "solra_dev" {
  metadata {
    name = "solra-dev"
    labels = {
      environment = "dev"
      managed-by  = "terraform"
    }
  }
}

# ── PostgreSQL (云托管) ──
# TODO: P1 — 根据目标云平台选择 RDS/Cloud SQL/PolarDB
# resource "aws_db_instance" "solra_dev" { ... }

# ── Redis (云托管) ──
# TODO: P1 — 根据目标云平台选择 ElastiCache/Memorystore
# resource "aws_elasticache_cluster" "solra_dev" { ... }

# ── EKS / GKE / ACK 集群引用 ──
# data "aws_eks_cluster" "solra" { ... }
