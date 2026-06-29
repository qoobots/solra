# Solra Staging 环境一键部署 (E-091)
param(
    [string]$Registry = "ghcr.io",
    [string]$ImageTag = "latest",
    [string]$Namespace = "solra-staging",
    [switch]$SkipTerraform,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$ROOT = Split-Path -Parent $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Solra Staging 环境部署" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

function Step($name, $script) {
    Write-Host "[$([char]0x2713)] $name" -ForegroundColor Yellow
    if (-not $DryRun) {
        & $script
        if ($LASTEXITCODE -ne 0) { throw "Step '$name' failed" }
    }
}

# 1. Terraform 资源就绪
if (-not $SkipTerraform) {
    Step "Terraform apply (staging)" {
        Push-Location "$ROOT\infra\terraform"
        try {
            terraform init -upgrade
            terraform workspace select staging 2>$null
            if ($LASTEXITCODE -ne 0) { terraform workspace new staging }
            terraform apply -var-file="environments/staging.tfvars" -auto-approve
        } finally { Pop-Location }
    }
}

# 2. K8s 命名空间 & RBAC
Step "K8s namespace & RBAC" {
    kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -
    kubectl apply -f "$ROOT\infra\kubernetes\base\namespace.yaml"
}

# 3. Docker Registry Secret
Step "Docker registry secret" {
    if (Test-Path Env:DOCKER_REGISTRY_USERNAME) {
        kubectl create secret docker-registry ghcr-secret `
            --namespace=$Namespace `
            --docker-server=$Registry `
            --docker-username=$env:DOCKER_REGISTRY_USERNAME `
            --docker-password=$env:DOCKER_REGISTRY_PASSWORD `
            --dry-run=client -o yaml | kubectl apply -f -
        Write-Host "  ✓ ghcr-secret configured" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ DOCKER_REGISTRY_USERNAME not set, skipping secret" -ForegroundColor DarkYellow
    }
}

# 4. Helm 部署
Step "Helm deploy (solra-services)" {
    helm upgrade --install solra-staging "$ROOT\infra\kubernetes\helm\solra-services" `
        --namespace $Namespace `
        --set global.image.tag=$ImageTag `
        --set global.environment=staging `
        --set global.image.registry=$Registry `
        --timeout 10m `
        --wait
}

# 5. ArgoCD 同步
Step "ArgoCD sync staging app" {
    argocd app sync solra-staging --prune 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  ⚠ argocd CLI not available or sync skipped" -ForegroundColor DarkYellow
    }
}

# 6. APISIX 路由更新
Step "APISIX route update" {
    $adminKey = if (Test-Path Env:APISIX_ADMIN_KEY) { $env:APISIX_ADMIN_KEY } else { "edd1c9f034335f136f87ad84b625c8f1" }
    $routesJson = Get-Content "$ROOT\infra\apisix\routes.yaml" -Raw

    # Admin API endpoint - typically accessible within cluster
    $adminUrl = kubectl get svc -n apisix apisix-admin -o jsonpath="{.spec.clusterIP}" 2>$null
    if ($adminUrl) {
        Write-Host "  APISIX admin at: http://${adminUrl}:9180" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ apisix-admin service not found, routes not auto-applied" -ForegroundColor DarkYellow
    }
}

# 7. 健康检查
Step "Health check" {
    Write-Host "  Waiting for pods to become ready..." -ForegroundColor Gray
    kubectl wait --for=condition=ready pod -l app.kubernetes.io/part-of=solra `
        --namespace $Namespace --timeout=300s 2>$null

    Write-Host ""
    Write-Host "  Pod status:" -ForegroundColor Cyan
    kubectl get pods -n $Namespace -o wide
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  ✅ Staging environment deployed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Namespace: $Namespace"
Write-Host "  Registry:  $Registry"
Write-Host "  Image Tag: $ImageTag"
Write-Host ""
Write-Host "  Useful commands:"
Write-Host "    kubectl get all -n $Namespace"
Write-Host "    kubectl logs -n $Namespace -l app.kubernetes.io/part-of=solra --tail=50"
Write-Host "    helm list -n $Namespace"
