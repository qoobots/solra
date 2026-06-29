#!/bin/bash
# ============================================================================
# solra prod 生产环境部署脚本
# ============================================================================
# 部署流程:
#   1. 预检查 (Terraform/Helm/kubectl/云CLI)
#   2. Terraform Apply (基础设施)
#   3. Helm Upgrade (K8s服务)
#   4. ArgoCD Sync (GitOps)
#   5. 健康检查 (等待所有Pod就绪)
#   6. 冒烟测试 (关键API验证)
#   7. 自动回滚 (失败时)
# ============================================================================

set -euo pipefail

# ============================================================================
# 配置
# ============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

: "${ENVIRONMENT:=prod}"
: "${CLOUD_PROVIDER:=aws}"          # aws / aliyun / tencent
: "${REGION:=us-east-1}"
: "${K8S_NAMESPACE:=solra-prod}"
: "${ARGOCD_SERVER:=argocd.solra.io}"
: "${HEALTH_CHECK_TIMEOUT:=600}"     # 10分钟超时
: "${ROLLBACK_ON_FAILURE:=true}"
: "${DRY_RUN:=false}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC}  $(date +'%H:%M:%S') $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date +'%H:%M:%S') $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date +'%H:%M:%S') $*"; }
log_step()  { echo -e "${BLUE}[STEP]${NC}  $(date +'%H:%M:%S') $*"; }

# ============================================================================
# 1. 预检查
# ============================================================================
preflight_checks() {
  log_step "1/7 预检..."

  local missing=()

  for cmd in terraform helm kubectl argocd jq curl; do
    if ! command -v "$cmd" &>/dev/null; then
      missing+=("$cmd")
    fi
  done

  case "$CLOUD_PROVIDER" in
    aws)     command -v aws     &>/dev/null || missing+=("aws") ;;
    aliyun)  command -v aliyun  &>/dev/null || missing+=("aliyun") ;;
    tencent) command -v tccli   &>/dev/null || missing+=("tccli") ;;
  esac

  if [ ${#missing[@]} -gt 0 ]; then
    log_error "缺少依赖: ${missing[*]}"
    exit 1
  fi

  # 验证环境名
  if [[ ! "$ENVIRONMENT" =~ ^(prod|staging|dev)$ ]]; then
    log_error "无效环境: $ENVIRONMENT (必须是 prod/staging/dev)"
    exit 1
  fi

  # prod 环境额外确认
  if [ "$ENVIRONMENT" = "prod" ] && [ "$DRY_RUN" != "true" ]; then
    echo -n "⚠️  确认部署到 PRODUCTION? 输入 'yes' 继续: "
    read -r confirm
    if [ "$confirm" != "yes" ]; then
      log_warn "部署已取消"
      exit 0
    fi
  fi

  log_info "预检通过 ✓"
}

# ============================================================================
# 2. Infrastructure as Code
# ============================================================================
terraform_apply() {
  log_step "2/7 Terraform Apply ($ENVIRONMENT)..."

  cd "$REPO_ROOT/infra/terraform"

  if [ "$DRY_RUN" = "true" ]; then
    terraform plan -var-file="environments/${ENVIRONMENT}.tfvars" \
      -out="${ENVIRONMENT}.tfplan"
    log_info "[DRY RUN] Terraform plan 已生成: ${ENVIRONMENT}.tfplan"
    return
  fi

  # 保存当前状态以便回滚
  terraform state pull > "/tmp/solra-tf-backup-${ENVIRONMENT}-$(date +%Y%m%d%H%M%S).tfstate"

  # Apply
  terraform apply -auto-approve \
    -var-file="environments/${ENVIRONMENT}.tfvars" \
    "${ENVIRONMENT}.tfplan" 2>/dev/null || \
    terraform apply -auto-approve \
      -var-file="environments/${ENVIRONMENT}.tfvars"

  log_info "Terraform Apply 完成 ✓"
}

# ============================================================================
# 3. Helm 部署
# ============================================================================
helm_deploy() {
  log_step "3/7 Helm Deploy (${K8S_NAMESPACE})..."

  # 确保命名空间存在
  kubectl get namespace "$K8S_NAMESPACE" &>/dev/null || \
    kubectl create namespace "$K8S_NAMESPACE"

  local services=(
    "auth-service"
    "avt-service"
    "spc-service"
    "saf-service"
    "soc-service"
    "grw-service"
    "crt-service"
    "not-service"
    "mon-service"
    "llm-router"
    "safety-model-service"
    "embedding-service"
    "recommendation-pipeline"
    "tts-service"
  )

  local failed_services=()

  for svc in "${services[@]}"; do
    log_info "  Deploying $svc..."

    if [ "$DRY_RUN" = "true" ]; then
      helm upgrade --install "$svc" "$REPO_ROOT/infra/kubernetes/helm/$svc" \
        -f "$REPO_ROOT/infra/kubernetes/helm/$svc/values.yaml" \
        -f "$REPO_ROOT/infra/kubernetes/helm/$svc/values-${ENVIRONMENT}.yaml" \
        -n "$K8S_NAMESPACE" --dry-run >/dev/null 2>&1
      log_info "    [DRY RUN] OK"
      continue
    fi

    if ! helm upgrade --install "$svc" "$REPO_ROOT/infra/kubernetes/helm/$svc" \
      -f "$REPO_ROOT/infra/kubernetes/helm/$svc/values.yaml" \
      -f "$REPO_ROOT/infra/kubernetes/helm/$svc/values-${ENVIRONMENT}.yaml" \
      -n "$K8S_NAMESPACE" --wait --timeout 5m \
      --set image.tag="${IMAGE_TAG:-latest}"; then
      failed_services+=("$svc")
      log_error "  ✗ $svc 部署失败"
    else
      log_info "  ✓ $svc"
    fi
  done

  if [ ${#failed_services[@]} -gt 0 ]; then
    log_error "失败的服务: ${failed_services[*]}"
    return 1
  fi

  log_info "Helm 部署完成 ✓"
}

# ============================================================================
# 4. ArgoCD Sync
# ============================================================================
argocd_sync() {
  log_step "4/7 ArgoCD Sync..."

  if [ "$DRY_RUN" = "true" ]; then
    log_info "[DRY RUN] 跳过 ArgoCD sync"
    return
  fi

  argocd login "$ARGOCD_SERVER" --grpc-web --sso 2>/dev/null || \
    argocd login "$ARGOCD_SERVER" --grpc-web --username admin \
      --password "${ARGOCD_PASSWORD:-}"

  argocd app sync "solra-${ENVIRONMENT}" --prune --timeout 600

  log_info "ArgoCD Sync 完成 ✓"
}

# ============================================================================
# 5. 健康检查
# ============================================================================
health_check() {
  log_step "5/7 健康检查 (超时 ${HEALTH_CHECK_TIMEOUT}s)..."

  if [ "$DRY_RUN" = "true" ]; then
    log_info "[DRY RUN] 跳过健康检查"
    return
  fi

  local start_time=$(date +%s)
  local deadline=$((start_time + HEALTH_CHECK_TIMEOUT))

  while [ $(date +%s) -lt $deadline ]; do
    # 检查所有 Pod 状态
    local not_ready=$(kubectl get pods -n "$K8S_NAMESPACE" \
      --no-headers 2>/dev/null | grep -vE 'Running|Completed' | wc -l)

    # 检查关键 Deployment
    local auth_ready=$(kubectl rollout status deployment/auth-service \
      -n "$K8S_NAMESPACE" --timeout=10s 2>/dev/null && echo 1 || echo 0)

    if [ "$not_ready" -eq 0 ] && [ "$auth_ready" -eq 1 ]; then
      log_info "所有 Pod 就绪 ✓"
      return 0
    fi

    log_info "等待中... (未就绪: $not_ready pods)"
    sleep 10
  done

  log_error "健康检查超时 (${HEALTH_CHECK_TIMEOUT}s)"
  return 1
}

# ============================================================================
# 6. 冒烟测试
# ============================================================================
smoke_test() {
  log_step "6/7 冒烟测试..."

  if [ "$DRY_RUN" = "true" ]; then
    log_info "[DRY RUN] 跳过冒烟测试"
    return
  fi

  # 获取 ingress 地址
  local ingress_host=$(kubectl get ingress solra-api -n "$K8S_NAMESPACE" \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || \
    echo "localhost")

  local base_url="https://${ingress_host}"

  local test_results=()

  # Test 1: 健康检查
  log_info "  测试 /healthz..."
  if curl -sf -o /dev/null -w "%{http_code}" "$base_url/healthz" | grep -q 200; then
    test_results+=("✓ healthz")
  else
    test_results+=("✗ healthz")
  fi

  # Test 2: Auth 服务
  log_info "  测试 auth-service..."
  if curl -sf -o /dev/null "$base_url/api/v1/auth/health"; then
    test_results+=("✓ auth")
  else
    test_results+=("✗ auth")
  fi

  # Test 3: 空间API
  log_info "  测试 space-service..."
  if curl -sf -o /dev/null "$base_url/api/v1/spaces?limit=1"; then
    test_results+=("✓ spaces")
  else
    test_results+=("✗ spaces")
  fi

  # Test 4: LLM Router
  log_info "  测试 llm-router..."
  if curl -sf -o /dev/null "$base_url/api/v1/ai/health"; then
    test_results+=("✓ llm")
  else
    test_results+=("✗ llm")
  fi

  echo ""
  log_info "冒烟测试结果:"
  for r in "${test_results[@]}"; do
    echo "  $r"
  done

  # 检查是否有失败
  local failures=$(printf '%s\n' "${test_results[@]}" | grep -c '✗' || true)
  if [ "$failures" -gt 0 ]; then
    log_error "冒烟测试 $failures 项失败"
    return 1
  fi

  log_info "冒烟测试通过 ✓"
}

# ============================================================================
# 7. 回滚 (失败时自动触发)
# ============================================================================
rollback() {
  local reason="${1:-unknown}"

  log_error "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  log_error "  部署失败: $reason"
  log_error "  自动回滚开始..."
  log_error "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  if [ "$DRY_RUN" = "true" ]; then
    log_info "[DRY RUN] 跳过回滚"
    return
  fi

  # Helm 回滚 (各服务回滚到上一版本)
  local services=(
    "auth-service" "avt-service" "spc-service" "saf-service"
    "soc-service" "grw-service" "crt-service" "not-service"
    "mon-service" "llm-router" "safety-model-service"
    "embedding-service" "recommendation-pipeline" "tts-service"
  )

  for svc in "${services[@]}"; do
    log_info "  回滚 $svc..."
    helm rollback "$svc" 0 -n "$K8S_NAMESPACE" --wait --timeout 3m 2>/dev/null || \
      log_warn "  $svc 无可回滚版本"
  done

  # Terraform 回滚 (恢复之前的状态)
  local latest_backup=$(ls -t /tmp/solra-tf-backup-prod-*.tfstate 2>/dev/null | head -1)
  if [ -n "$latest_backup" ]; then
    log_info "  恢复 Terraform 状态: $latest_backup"
    terraform state push "$latest_backup"
  fi

  log_info "回滚完成"
  exit 1
}

# ============================================================================
# 主流程
# ============================================================================
main() {
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  Solra Production Deploy"
  echo "  环境: $ENVIRONMENT | 云: $CLOUD_PROVIDER"
  echo "  时间: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  [ "$DRY_RUN" = "true" ] && echo "  模式: DRY RUN (仅检查)"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""

  # 1. 预检
  preflight_checks || exit 1

  # 2. Terraform
  if ! terraform_apply; then
    rollback "Terraform Apply failed"
  fi

  # 3. Helm
  if ! helm_deploy; then
    if [ "$ROLLBACK_ON_FAILURE" = "true" ]; then
      rollback "Helm 部署失败"
    else
      log_error "Helm 部署失败 (rollback disabled)"
      exit 1
    fi
  fi

  # 4. ArgoCD
  argocd_sync || log_warn "ArgoCD sync 失败, 继续..."

  # 5. 健康检查
  if ! health_check; then
    if [ "$ROLLBACK_ON_FAILURE" = "true" ]; then
      rollback "健康检查失败"
    else
      log_error "健康检查失败 (rollback disabled)"
      exit 1
    fi
  fi

  # 6. 冒烟测试
  if ! smoke_test; then
    if [ "$ROLLBACK_ON_FAILURE" = "true" ]; then
      rollback "冒烟测试失败"
    else
      log_error "冒烟测试失败 (rollback disabled)"
      exit 1
    fi
  fi

  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  ✅ 生产环境部署成功"
  echo "  环境: $ENVIRONMENT"
  echo "  耗时: ${SECONDS}s"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  # 记录部署事件
  if [ "$DRY_RUN" != "true" ]; then
    git tag "deploy-${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S)"
  fi
}

# 运行
main "$@"
