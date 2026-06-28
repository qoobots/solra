# Solra工程目录初始化脚本
$root = "d:\05workspaces\solra"

$dirs = @(
    # === contracts/ ===
    "contracts/proto/common",
    "contracts/proto/avt",
    "contracts/proto/spc",
    "contracts/proto/crt",
    "contracts/proto/soc",
    "contracts/proto/grw",
    "contracts/proto/mon",
    "contracts/proto/auth",
    "contracts/proto/saf",
    "contracts/proto/not",
    "contracts/events",
    "contracts/openapi",

    # === core/ ===
    "core/cmake",
    "core/include/solra/core",
    "core/include/solra/render",
    "core/include/solra/inference",
    "core/include/solra/webrtc",
    "core/include/solra/streaming",
    "core/include/solra/audio",
    "core/include/solra/animation",
    "core/include/solra/storage",
    "core/include/solra/platform",
    "core/src/core",
    "core/src/render",
    "core/src/inference",
    "core/src/webrtc",
    "core/src/streaming",
    "core/src/audio",
    "core/src/animation",
    "core/src/storage",
    "core/src/platform/ios",
    "core/src/platform/android",
    "core/src/platform/desktop",
    "core/third_party",
    "core/tests/integration",
    "core/tests/performance",

    # === services/ ===
    "services/common/common-core/src/main/java/com/solra/common/core/model",
    "services/common/common-core/src/main/java/com/solra/common/core/util",
    "services/common/common-core/src/main/java/com/solra/common/core/exception",
    "services/common/common-core/src/main/java/com/solra/common/core/config",
    "services/common/common-event/src/main/java/com/solra/common/event",
    "services/common/common-grpc/src/main/java/com/solra/common/grpc/interceptor",
    "services/common/common-grpc/src/main/java/com/solra/common/grpc/config",

    # services/avt-service/
    "services/avt-service/conversation-svc",
    "services/avt-service/avatar-mgmt-svc",
    "services/avt-service/emotion-svc",
    "services/avt-service/memory-svc",

    # services/spc-service/
    "services/spc-service/recommend-svc",
    "services/spc-service/streaming-svc",
    "services/spc-service/search-svc",

    # services/crt-service/
    "services/crt-service/editor-svc",
    "services/crt-service/asset-svc",
    "services/crt-service/publish-svc",

    # services/soc-service/
    "services/soc-service/sync-svc",
    "services/soc-service/chat-svc",
    "services/soc-service/social-graph-svc",

    # services/grw-service/
    "services/grw-service/presence-svc",
    "services/grw-service/faith-svc",
    "services/grw-service/onboarding-svc",

    # services/mon-service/
    "services/mon-service/subscription-svc",
    "services/mon-service/marketplace-svc",
    "services/mon-service/payment-svc",

    # services/auth-service/
    "services/auth-service/auth-svc",

    # services/saf-service/
    "services/saf-service/safety-filter-svc",
    "services/saf-service/review-workbench-svc",

    # services/not-service/
    "services/not-service/push-svc",
    "services/not-service/notification-svc",

    # === ai-services/ ===
    "ai-services/llm-router/src/api",
    "ai-services/llm-router/src/core",
    "ai-services/llm-router/src/inference",
    "ai-services/llm-router/src/models",
    "ai-services/llm-router/src/config",
    "ai-services/llm-router/src/tests",
    "ai-services/llm-router/src/__init__.py",
    "ai-services/embedding-service/src/api",
    "ai-services/embedding-service/src/core",
    "ai-services/embedding-service/src/config",
    "ai-services/embedding-service/src/tests",
    "ai-services/tts-service/src/api",
    "ai-services/tts-service/src/core",
    "ai-services/tts-service/src/config",
    "ai-services/tts-service/src/tests",
    "ai-services/safety-model-service/src/api",
    "ai-services/safety-model-service/src/core",
    "ai-services/safety-model-service/src/config",
    "ai-services/safety-model-service/src/tests",
    "ai-services/recommendation-pipeline/src/api",
    "ai-services/recommendation-pipeline/src/training",
    "ai-services/recommendation-pipeline/src/serving",
    "ai-services/recommendation-pipeline/src/config",
    "ai-services/recommendation-pipeline/src/tests",
    "ai-services/models/.dvc",
    "ai-services/models/llm",
    "ai-services/models/embedding",
    "ai-services/models/tts",
    "ai-services/models/safety",
    "ai-services/models/recommendation",
    "ai-services/shared/src/solra_ai_shared",

    # === clients/ ===
    # clients/ios/
    "clients/ios/Solra/App",
    "clients/ios/Solra/Features/Space",
    "clients/ios/Solra/Features/Avatar",
    "clients/ios/Solra/Features/Social",
    "clients/ios/Solra/Features/Growth",
    "clients/ios/Solra/Features/Creation",
    "clients/ios/Solra/Features/Commerce",
    "clients/ios/Solra/Features/Auth",
    "clients/ios/Solra/Features/Settings",
    "clients/ios/Solra/Core/Network",
    "clients/ios/Solra/Core/Storage",
    "clients/ios/Solra/Core/Audio",
    "clients/ios/Solra/Core/Analytics",
    "clients/ios/Solra/Generated/avt",
    "clients/ios/Solra/Generated/spc",
    "clients/ios/Solra/Resources/Fonts",
    "clients/ios/Solra/Resources/Localization/en.lproj",
    "clients/ios/Solra/Resources/Localization/zh-Hans.lproj",
    "clients/ios/Solra/Resources/Sounds",
    "clients/ios/Solra/DesignSystem/Components",
    "clients/ios/SolraTests/ViewModelTests",
    "clients/ios/SolraTests/ServiceTests",
    "clients/ios/SolraUITests",

    # clients/android/
    "clients/android/gradle",
    "clients/android/app/src/main/kotlin/com/solra/android/app/di",
    "clients/android/app/src/main/kotlin/com/solra/android/features/space",
    "clients/android/app/src/main/kotlin/com/solra/android/features/avatar",
    "clients/android/app/src/main/kotlin/com/solra/android/features/social",
    "clients/android/app/src/main/kotlin/com/solra/android/features/growth",
    "clients/android/app/src/main/kotlin/com/solra/android/features/creation",
    "clients/android/app/src/main/kotlin/com/solra/android/features/commerce",
    "clients/android/app/src/main/kotlin/com/solra/android/features/auth",
    "clients/android/app/src/main/kotlin/com/solra/android/features/settings",
    "clients/android/app/src/main/kotlin/com/solra/android/core/network",
    "clients/android/app/src/main/kotlin/com/solra/android/core/storage",
    "clients/android/app/src/main/kotlin/com/solra/android/core/audio",
    "clients/android/app/src/main/kotlin/com/solra/android/core/analytics",
    "clients/android/app/src/main/kotlin/com/solra/android/design_system/components",
    "clients/android/app/src/main/res/values",
    "clients/android/app/src/main/res/drawable",
    "clients/android/app/src/main/res/font",
    "clients/android/app/src/main/res/raw",
    "clients/android/app/src/main/proto",
    "clients/android/app/src/test/kotlin/com/solra/android/features",
    "clients/android/app/src/test/kotlin/com/solra/android/core",
    "clients/android/app/src/androidTest/kotlin/com/solra/android",

    # clients/web/
    "clients/web/public",
    "clients/web/src/app",
    "clients/web/src/features/space",
    "clients/web/src/features/avatar",
    "clients/web/src/features/social",
    "clients/web/src/features/growth",
    "clients/web/src/features/creation",
    "clients/web/src/features/commerce",
    "clients/web/src/features/auth",
    "clients/web/src/features/settings",
    "clients/web/src/core/api",
    "clients/web/src/core/storage",
    "clients/web/src/core/analytics",
    "clients/web/src/design-system/tokens",
    "clients/web/src/design-system/components/Button",
    "clients/web/src/design-system/components/TextField",
    "clients/web/src/design-system/components/LoadingIndicator",
    "clients/web/src/design-system/components/Toast",
    "clients/web/src/generated/avt",
    "clients/web/src/generated/spc",
    "clients/web/src/utils",

    # === infra/ ===
    "infra/terraform/modules/vpc",
    "infra/terraform/modules/eks",
    "infra/terraform/modules/rds",
    "infra/terraform/modules/redis",
    "infra/terraform/modules/pulsar",
    "infra/terraform/modules/object-storage",
    "infra/terraform/modules/cdn",
    "infra/terraform/environments/dev",
    "infra/terraform/environments/staging",
    "infra/terraform/environments/prod",
    "infra/kubernetes/base",
    "infra/kubernetes/charts/solra-services/templates",
    "infra/kubernetes/charts/solra-ai/templates",
    "infra/kubernetes/charts/solra-infra/templates",
    "infra/kubernetes/argocd/projects",
    "infra/kubernetes/argocd/applications",
    "infra/apisix/routes",
    "infra/apisix/upstreams",
    "infra/apisix/plugins",
    "infra/apisix/ssl",
    "infra/docker/docker-compose/init-scripts",
    "infra/monitoring/prometheus/rules",
    "infra/monitoring/prometheus/scrape-configs",
    "infra/monitoring/grafana/dashboards",
    "infra/monitoring/grafana/datasources",
    "infra/monitoring/jaeger",
    "infra/monitoring/elk",

    # === tools/ ===
    "tools/codegen",
    "tools/dev-env/seed-data",
    "tools/db-migration",
    "tools/load-test/k6",
    "tools/load-test/locust",
    "tools/release",
    "tools/ide",

    # === .github/ ===
    ".github/workflows",
    ".github/actions/setup-java",
    ".github/actions/setup-python",
    ".github/actions/setup-cpp",
    ".github/ISSUE_TEMPLATE"
)

foreach ($dir in $dirs) {
    $fullPath = Join-Path $root $dir
    if (-not (Test-Path $fullPath)) {
        New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
        # Create .gitkeep in empty leaf directories to track structure
    }
}

Write-Host "All directories created successfully." -ForegroundColor Green
Write-Host "Total directory entries: $($dirs.Count)" -ForegroundColor Cyan
