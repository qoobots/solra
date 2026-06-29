# prod 环境变量
environment = "prod"
region      = "cn-hangzhou"
domain_name = "solra.io"

k8s_worker_count = 5
k8s_worker_type  = "ecs.g7.2xlarge"  # 8C32G
db_storage_gb    = 200
redis_capacity_gb = 16
