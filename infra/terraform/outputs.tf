# Solra Terraform 资源输出

output "vpc_id" {
  value = module.network.vpc_id
}

output "k8s_cluster_endpoint" {
  value     = module.kubernetes.endpoint
  sensitive = true
}

output "k8s_cluster_ca" {
  value     = module.kubernetes.cluster_ca_certificate
  sensitive = true
}

output "database_endpoint" {
  value     = module.database.endpoint
  sensitive = true
}

output "redis_endpoint" {
  value     = module.cache.endpoint
  sensitive = true
}

output "cdn_domain" {
  value = module.cdn.domain
}
