# Solra Terraform — 多云资源编排
#
# 支持的云平台：阿里云 (primary) / AWS / 腾讯云（按需扩展）

terraform {
  required_version = ">= 1.6"
  required_providers {
    alicloud = {
      source  = "aliyun/alicloud"
      version = "~> 1.220"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
    tencentcloud = {
      source  = "tencentcloudstack/tencentcloud"
      version = "~> 1.81"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
  }

  backend "s3" {
    bucket = "solra-tfstate"
    key    = "terraform.tfstate"
    region = "cn-hangzhou"
  }
}

# ============================================================
# 模块：网络基础设施
# ============================================================
module "network" {
  source = "./modules/network"

  vpc_cidr   = var.vpc_cidr
  region     = var.region
  environment = var.environment
}

# ============================================================
# 模块：Kubernetes 集群
# ============================================================
module "kubernetes" {
  source = "./modules/kubernetes"

  vpc_id          = module.network.vpc_id
  vswitch_ids     = module.network.vswitch_ids
  cluster_version = var.k8s_version
  worker_count    = var.k8s_worker_count
  worker_type     = var.k8s_worker_type
  environment     = var.environment
}

# ============================================================
# 模块：数据库 (RDS)
# ============================================================
module "database" {
  source = "./modules/database"

  vpc_id      = module.network.vpc_id
  vswitch_ids = module.network.vswitch_ids
  environment = var.environment
}

# ============================================================
# 模块：缓存 (Redis)
# ============================================================
module "cache" {
  source = "./modules/cache"

  vpc_id      = module.network.vpc_id
  vswitch_ids = module.network.vswitch_ids
  environment = var.environment
}

# ============================================================
# 模块：消息队列 (Kafka)
# ============================================================
module "messaging" {
  source = "./modules/messaging"

  vpc_id      = module.network.vpc_id
  vswitch_ids = module.network.vswitch_ids
  environment = var.environment
}

# ============================================================
# 模块：对象存储 (OSS/S3/COS)
# ============================================================
module "storage" {
  source = "./modules/storage"

  environment = var.environment
}

# ============================================================
# 模块：CDN & DNS
# ============================================================
module "cdn" {
  source = "./modules/cdn"

  domain_name = var.domain_name
  environment = var.environment
}
