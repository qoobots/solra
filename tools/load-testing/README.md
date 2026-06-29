# Solra Load Testing - README

## 目录结构

```
tools/load-testing/
├── k6/
│   ├── auth_test.js          # 认证服务 (登录+Token刷新)
│   ├── space_test.js         # 空间服务 (推荐流+详情+搜索)
│   ├── websocket_test.js     # WebSocket实时场景 (社交在场)
│   └── scenarios/
│       ├── full_stack.js     # 全链路混合场景
│       └── ai_inference.js   # AI推理服务场景
├── gatling/                   # (未来) Java压测
│   └── ...
├── Makefile
├── run-all.sh
└── README.md
```

## 快速开始

```bash
# 安装 K6 (https://k6.io/docs/get-started/installation/)
brew install k6        # macOS
choco install k6       # Windows
sudo apt install k6    # Linux

# 运行单个场景
k6 run k6/auth_test.js -e TARGET_URL=https://dev.solra.io

# 指定场景类型
k6 run k6/auth_test.js -e K6_SCENARIO=smoke

# 运行全部
./run-all.sh
```

## 性能基准 (SLO)

| 服务 | 场景 | p95延迟 | 错误率 | 目标RPS |
|------|------|---------|--------|---------|
| auth-service | login | <200ms | <0.1% | 1000 |
| spc-service | recommend | <300ms | <0.5% | 500 |
| spc-service | search | <400ms | <1% | 200 |
| soc-service | ws pos-update | <50ms | <0.1% | 5000 |
| llm-router | chat | <2000ms | <1% | 50 |
| saf-service | text-check | <100ms | <0.1% | 2000 |
