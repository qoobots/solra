#!/bin/bash
# Solra Dev Environment — Linux/macOS version
set -euo pipefail

ENV="${1:-dev}"
SKIP_INFRA="${2:-false}"
SKIP_BUILD="${3:-false}"

echo "=== Solra Deployment ($ENV) ==="

# Pre-flight
command -v docker  >/dev/null || { echo "Docker not found"; exit 1; }
command -v kubectl >/dev/null || { echo "kubectl not found"; exit 1; }
command -v helm    >/dev/null || { echo "helm not found"; exit 1; }

# Infrastructure
if [ "$SKIP_INFRA" != "true" ]; then
    echo "[*] Starting infra..."
    docker-compose -f infra/docker/docker-compose.yml up -d postgres redis kafka minio
    sleep 10
fi

# Proto gen
echo "[*] Generating Proto..."
(cd contracts && buf generate)

# Build
if [ "$SKIP_BUILD" != "true" ]; then
    echo "[*] Building services..."
    ./gradlew :services:auth-service:build -x test
    for svc in auth-service avt-service spc-service soc-service crt-service grw-service not-service saf-service mon-service; do
        docker build -t "solra/$svc:dev" "services/$svc/" 2>/dev/null || true
    done
fi

# Deploy
echo "[*] Deploying..."
kubectl create namespace solra --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install "solra-$ENV" infra/kubernetes/helm/solra-services \
    --namespace solra \
    --set global.imageTag=dev \
    --wait --timeout 5m

echo "=== Done ==="
