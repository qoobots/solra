#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Solra 工程一键开发环境初始化脚本
.DESCRIPTION
    自动检测并安装所有必需的开发工具、运行时和依赖项。
    支持 Windows / macOS / Linux 三平台。
.NOTES
    P0 工程任务 E-035
#>

param(
    [switch]$SkipCheck,        # 跳过环境检查，强制安装
    [switch]$DryRun,           # 仅检查，不安装
    [string]$Components = "all" # 可选: all|services|ai|core|clients|infra|contracts
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\..\.."

# ═══════════════════════════════════════════════════════════════
# 0. 颜色输出辅助
# ═══════════════════════════════════════════════════════════════

function Write-Step { param($Msg) Write-Host "`n>>> $Msg" -ForegroundColor Cyan }
function Write-Ok { param($Msg) Write-Host "  ✔ $Msg" -ForegroundColor Green }
function Write-Warn { param($Msg) Write-Host "  ⚠ $Msg" -ForegroundColor Yellow }
function Write-Err { param($Msg) Write-Host "  ✘ $Msg" -ForegroundColor Red }
function Write-Info { param($Msg) Write-Host "  ℹ $Msg" -ForegroundColor Gray }

# ═══════════════════════════════════════════════════════════════
# 1. 操作系统检测
# ═══════════════════════════════════════════════════════════════

$IsWindows = $PSVersionTable.PSVersion.Major -ge 5 -and $IsWindows -ne $false -or $env:OS -eq "Windows_NT" -or [System.Environment]::OSVersion.Platform -eq "Win32NT"
$IsMacOS = $IsMacOS -or [System.Environment]::OSVersion.Platform -eq "Unix" -and (uname -s 2>$null) -eq "Darwin"
$IsLinux = $IsLinux -or [System.Environment]::OSVersion.Platform -eq "Unix" -and (uname -s 2>$null) -eq "Linux"

Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  索拉工程 (Solra) 开发环境初始化工具       ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Info "Project Root: $ProjectRoot"
Write-Info "OS: $(if ($IsWindows) {'Windows'} elseif ($IsMacOS) {'macOS'} elseif ($IsLinux) {'Linux'} else {'Unknown'})"
Write-Info "Components: $Components"

# ═══════════════════════════════════════════════════════════════
# 2. 通用工具检查与安装
# ═══════════════════════════════════════════════════════════════

function Check-Command {
    param($Name, $CheckCmd, $InstallHint, [string]$MinVersion = "")
    Write-Info "Checking $Name ..."
    try {
        $result = Invoke-Expression $CheckCmd 2>&1
        if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE) {
            Write-Warn "$Name not found. Install: $InstallHint"
            return $false
        }
        Write-Ok "$Name found"
        return $true
    } catch {
        Write-Warn "$Name not found. Install: $InstallHint"
        return $false
    }
}

Write-Step "Checking common tools"

# Git
Check-Command "Git" "git --version" "https://git-scm.com/downloads"

# JDK 17 (for services/)
if ($Components -eq "all" -or $Components -eq "services") {
    Check-Command "Java" "java -version 2>&1" "https://adoptium.net/download/ (JDK 17+)"
}

# Python 3.11 (for ai-services/)
if ($Components -eq "all" -or $Components -eq "ai") {
    Check-Command "Python" "python --version 2>&1" "https://www.python.org/downloads/ (3.11+)"
    Check-Command "pip" "pip --version" "Comes with Python"
}

# CMake (for core/)
if ($Components -eq "all" -or $Components -eq "core") {
    Check-Command "CMake" "cmake --version" "https://cmake.org/download/ (3.24+)"
}

# Docker (for infra/)
if ($Components -eq "all" -or $Components -eq "infra") {
    Check-Command "Docker" "docker --version" "https://www.docker.com/products/docker-desktop/"
}

# buf (for contracts/)
if ($Components -eq "all" -or $Components -eq "contracts") {
    Check-Command "buf" "buf --version" "https://buf.build/docs/installation"
}

# ═══════════════════════════════════════════════════════════════
# 3. 项目依赖安装
# ═══════════════════════════════════════════════════════════════

if ($DryRun) {
    Write-Step "Dry run mode - skipping dependency installation"
    Write-Info "All checks completed. Exiting."
    exit 0
}

# ── contracts/ Proto 依赖 ──
if ($Components -eq "all" -or $Components -eq "contracts") {
    Write-Step "Initializing contracts/ (Proto)"
    Push-Location "$ProjectRoot/contracts/proto"
    try {
        buf mod update 2>&1 | Write-Info
        Write-Ok "Proto dependencies updated"
    } catch {
        Write-Warn "buf mod update failed (may need buf installed and logged in)"
    }
    Pop-Location
}

# ── core/ C++ 依赖 ──
if ($Components -eq "all" -or $Components -eq "core") {
    Write-Step "Installing core/ C++ dependencies (vcpkg)"
    Push-Location "$ProjectRoot/core"

    # Check vcpkg
    $vcpkgRoot = $env:VCPKG_ROOT
    if (-not $vcpkgRoot) {
        $vcpkgRoot = "$env:USERPROFILE/vcpkg"
        if (-not (Test-Path $vcpkgRoot)) {
            Write-Info "Cloning vcpkg to $vcpkgRoot ..."
            git clone https://github.com/microsoft/vcpkg.git $vcpkgRoot
        }
    }

    $bootstrap = if ($IsWindows) { "bootstrap-vcpkg.bat" } else { "bootstrap-vcpkg.sh" }
    & "$vcpkgRoot/$bootstrap" -disableMetrics 2>&1 | Write-Info

    Write-Info "Installing vcpkg dependencies from vcpkg.json ..."
    & "$vcpkgRoot/vcpkg" install --feature-flags=manifests 2>&1 | Write-Info

    Write-Info "Configuring CMake ..."
    New-Item -ItemType Directory -Force -Path build | Out-Null
    Push-Location build
    try {
        cmake .. -G Ninja -DCMAKE_BUILD_TYPE=Debug -DCMAKE_TOOLCHAIN_FILE="$vcpkgRoot/scripts/buildsystems/vcpkg.cmake" 2>&1 | Write-Info
        Write-Ok "Core CMake configured"
    } catch {
        Write-Warn "CMake configure failed - check vcpkg dependencies"
    }
    Pop-Location
    Pop-Location
}

# ── services/ Java 依赖 ──
if ($Components -eq "all" -or $Components -eq "services") {
    Write-Step "Installing services/ Java dependencies (Gradle)"
    Push-Location $ProjectRoot
    try {
        ./gradlew --no-daemon dependencies 2>&1 | Select-Object -Last 20 | Write-Info
        Write-Ok "Gradle dependencies resolved"
    } catch {
        Write-Warn "Gradle build failed - check Java 17+ installation"
    }
    Pop-Location
}

# ── ai-services/ Python 依赖 ──
if ($Components -eq "all" -or $Components -eq "ai") {
    Write-Step "Installing ai-services/ Python dependencies"
    Push-Location $ProjectRoot
    try {
        if (Test-Path "ai-services/requirements-dev.txt") {
            pip install -r ai-services/requirements-dev.txt 2>&1 | Write-Info
        }
        # Install each service
        $aiDirs = @("ai-services/llm-router", "ai-services/safety-model-service")
        foreach ($dir in $aiDirs) {
            if (Test-Path "$dir/pyproject.toml") {
                Write-Info "Installing $dir ..."
                Push-Location $dir
                pip install -e ".[dev]" 2>&1 | Write-Info
                Pop-Location
            }
        }
        Write-Ok "Python dependencies installed"
        Write-Info "Installing shared middleware as editable ..."
        if (Test-Path "ai-services/shared") {
            pip install -e "ai-services/shared" 2>&1 | Write-Info
        }
        Write-Ok "Shared middleware installed"
    } catch {
        Write-Warn "Python dependency install failed"
    }
    Pop-Location
}

# ═══════════════════════════════════════════════════════════════
# 4. 环境变量提示
# ═══════════════════════════════════════════════════════════════

Write-Step "Environment Configuration"
Write-Info "Recommended environment variables:"
Write-Info "  VCPKG_ROOT    = $env:USERPROFILE/vcpkg"
Write-Info "  JAVA_HOME     = /path/to/jdk-17"
Write-Info "  GITHUB_TOKEN  = (for CI local testing)"

# ═══════════════════════════════════════════════════════════════
# 5. 完成
# ═══════════════════════════════════════════════════════════════

Write-Host ""
Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  🎉 Solra 开发环境初始化完成！            ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Info "Quick start commands:"
Write-Info "  contracts:   cd contracts/proto && buf lint"
Write-Info "  core:        cd core/build && cmake --build ."
Write-Info "  services:    ./gradlew :services:auth-service:bootRun"
Write-Info "  ai-services: cd ai-services/llm-router && python src/main.py"
Write-Info "  tools:       pwsh tools/scripts/init-dirs.ps1"
Write-Host ""
