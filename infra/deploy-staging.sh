#!/usr/bin/env bash
# Solra Staging 环境一键部署 (E-091)
set -euo pipefail

REGISTRY="${REGISTRY:-ghcr.io}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
NAMESPACE="${NAMESPACE:-solra-staging}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DRY_RUN=false
SKIP_TF=false

while [[ $# -gt 0 ]]; do
  case $1 in
    --dry-run) DRY_RUN=true; shift ;;
    --skip-terraform) SKIP_TF=true; shift ;;
    --tag) IMAGE_TAG="$2"; shift 2 ;;
    *) echo "Unknown: $1"; exit 1 ;;
  esac
done

echo -e "\033[36m========================================\033[0m"
echo -e "\033[36m  Solra Staging 环境部署\033[0m"
echo -e "\033[36m========================================\033[0m"
echo ""

step() {
  echo -e "\033[33m[✓] $1\033[0m"
}

run() {
  if [ "$DRY_RUN" = true ]; then
    echo "  [DRY-RUN] $*"
  else
    eval "$*"
  fi
}

# 1. Terraform
if [ "$SKIP_TF" = false ]; then
  step "Terraform apply (staging)"
  run "cd $ROOT/infra/terraform && terraform init -upgrade"
  run "cd $ROOT/infra/terraform && terraform workspace select staging 2>/dev/null || terraform workspace new staging"
  run "cd $ROOT/infra/terraform && terraform apply -var-file=environments/staging.tfvars -auto-approve"
fi

# 2. K8s namespace
step "K8s namespace & RBAC"
run "kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -"

# 3. Docker registry secret
step "Docker registry secret"
if [ -n "${DOCKER_REGISTRY_USERNAME:-}" ]; then
  run "kubectl create secret docker-registry ghcr-secret \
    --namespace=$NAMESPACE \
    --docker-server=$REGISTRY \
    --docker-username=$DOCKER_REGISTRY_USERNAME \
    --docker-password=${DOCKER_REGISTRY_PASSWORD:-} \
    --dry-run=client -o yaml | kubectl apply -f -"
  echo -e "  \033[32m✓ ghcr-secret configured\033[0m"
else
  echo -e "  \033[33m⚠ DOCKER_REGISTRY_USERNAME not set\033[0m"
fi

# 4. Helm deploy
step "Helm deploy (solra-services)"
run "helm upgrade --install solra-staging $ROOT/infra/kubernetes/helm/solra-services \
  --namespace $NAMESPACE \
  --set global.image.tag=$IMAGE_TAG \
  --set global.environment=staging \
  --set global.image.registry=$REGISTRY \
  --timeout 10m \
  --wait"

# 5. ArgoCD sync
step "ArgoCD sync"
if command -v argocd &> /dev/null; then
  run "argocd app sync solra-staging --prune"
else
  echo -e "  \033[33m⚠ argocd CLI not available\033[0m"
fi

# 6. Health check
step "Health check"
echo "  Waiting for pods..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=solra \
  --namespace "$NAMESPACE" --timeout=300s 2>/dev/null || true
echo ""
kubectl get pods -n "$NAMESPACE" -o wide

echo ""
echo -e "\033[32m========================================\033[0m"
echo -e "\033[32m  ✅ Staging environment deployed!\033[0m"
echo -e "\033[32m========================================\033[0m"
echo ""
echo "  Namespace: $NAMESPACE"
echo "  Image Tag: $IMAGE_TAG"
