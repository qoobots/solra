# Solra 开发环境一键部署
#
# 用法:
#   powershell -ExecutionPolicy Bypass -File deploy-dev.ps1
#   powershell -ExecutionPolicy Bypass -File deploy-dev.ps1 -SkipInfra

param(
    [switch]$SkipInfra,
    [switch]$SkipBuild,
    [ValidateSet("dev", "staging")]
    [string]$Env = "dev"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "=== Solra Development Environment Deploy ===" -ForegroundColor Cyan
Write-Host "  Environment: $Env"
Write-Host "  SkipInfra: $SkipInfra"
Write-Host "  SkipBuild: $SkipBuild"
Write-Host ""

# ======================== 1. 前置检查 ========================
Write-Host "[1/6] Pre-flight checks..." -ForegroundColor Yellow

$required = @(
    @{Name="docker"; Check="docker --version"},
    @{Name="kubectl"; Check="kubectl version --client"},
    @{Name="helm"; Check="helm version"},
    @{Name="java"; Check="java --version"},
    @{Name="python"; Check="python --version"}
)

foreach ($tool in $required) {
    $ok = $true
    try { Invoke-Expression $tool.Check *>$null } catch { $ok = $false }
    if ($ok) {
        Write-Host "  [OK] $($tool.Name)" -ForegroundColor Green
    } else {
        Write-Host "  [MISSING] $($tool.Name) — install first" -ForegroundColor Red
        exit 1
    }
}

# ======================== 2. 基础设施 ========================
if (-not $SkipInfra) {
    Write-Host "[2/6] Starting infrastructure (Docker Compose)..." -ForegroundColor Yellow

    docker-compose -f infra/docker/docker-compose.yml up -d postgres redis kafka minio
    Write-Host "  Waiting for services to be healthy..."
    Start-Sleep -Seconds 10

    # 验证
    docker-compose -f infra/docker/docker-compose.yml ps
    Write-Host "  [OK] Infrastructure ready" -ForegroundColor Green
} else {
    Write-Host "[2/6] Skipping infrastructure" -ForegroundColor Gray
}

# ======================== 3. Proto 代码生成 ========================
Write-Host "[3/6] Proto code generation..." -ForegroundColor Yellow

Push-Location contracts
try {
    buf generate
    Write-Host "  [OK] Proto → Java/Python/TypeScript generated" -ForegroundColor Green
} finally {
    Pop-Location
}

# ======================== 4. 构建 ========================
if (-not $SkipBuild) {
    Write-Host "[4/6] Building services..." -ForegroundColor Yellow

    # Java 微服务
    ./gradlew :services:auth-service:build :services:avt-service:build `
              :services:spc-service:build :services:soc-service:build `
              :services:crt-service:build :services:grw-service:build `
              :services:not-service:build :services:saf-service:build `
              :services:mon-service:build -x test

    # Docker 镜像
    $svcList = @("auth-service","avt-service","spc-service","soc-service","crt-service","grw-service","not-service","saf-service","mon-service")
    foreach ($svc in $svcList) {
        docker build -t solra/$svc:dev services/$svc/
    }

    # AI 服务
    $aiSvcList = @("llm-router","safety-model-service","embedding-service","recommendation-pipeline","tts-service","gpu-scheduler")
    foreach ($svc in $aiSvcList) {
        docker build -t solra/$svc:dev ai-services/$svc/ 2>$null
    }

    Write-Host "  [OK] All services built" -ForegroundColor Green
} else {
    Write-Host "[4/6] Skipping build" -ForegroundColor Gray
}

# ======================== 5. K8s 部署 ========================
Write-Host "[5/6] Deploying to Kubernetes..." -ForegroundColor Yellow

# 创建命名空间
kubectl create namespace solra --dry-run=client -o yaml | kubectl apply -f -

# Helm 部署
helm upgrade --install solra-$Env infra/kubernetes/helm/solra-services `
    --namespace solra `
    -f infra/kubernetes/helm/solra-services/values.yaml `
    --set global.imageTag=dev `
    --set global.imagePullPolicy=Never `
    --wait --timeout 5m

Write-Host "  [OK] Deployed to K8s" -ForegroundColor Green

# ======================== 6. 状态检查 ========================
Write-Host "[6/6] Verifying deployment..." -ForegroundColor Yellow

kubectl get pods -n solra -o wide
kubectl get svc -n solra
kubectl get ingress -n solra

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host "  Health check: curl http://localhost:8080/health"
Write-Host "  Swagger UI:  http://localhost:8080/swagger-ui.html"
Write-Host "  Grafana:     http://localhost:3000  (admin/admin)"
