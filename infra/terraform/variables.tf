# Solra Terraform 变量定义

# ---- 通用 ----
variable "environment" {
  description = "部署环境: dev / staging / prod"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "云服务区域"
  type        = string
  default     = "cn-hangzhou"
}

variable "domain_name" {
  description = "服务域名"
  type        = string
  default     = "solra.io"
}

# ---- 网络 ----
variable "vpc_cidr" {
  default = "10.0.0.0/16"
}

# ---- Kubernetes ----
variable "k8s_version" {
  default = "1.28"
}

variable "k8s_worker_count" {
  description = "Worker 节点数"
  type        = number
  default     = 3
}

variable "k8s_worker_type" {
  description = "Worker 节点规格"
  type        = string
  default     = "ecs.g7.2xlarge"  # 8C32G
}

# ---- 数据库 ----
variable "db_instance_class" {
  default     = "pg.n4.2c.2m"
}
variable "db_storage_gb" {
  type    = number
  default = 100
}

# ---- Redis ----
variable "redis_instance_class" {
  default     = "redis.master.small.default"
}
variable "redis_capacity_gb" {
  type    = number
  default = 4
}
