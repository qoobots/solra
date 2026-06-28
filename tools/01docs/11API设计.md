# 开发工具链（Tools）API设计

> tools/ 的"API"——Makefile命令与脚本参数约定

---

## 一、Makefile 命令清单

```makefile
# 开发环境
make dev-setup          # 首次环境搭建
make dev-start          # 启动开发环境
make dev-stop           # 停止开发环境
make dev-status         # 查看环境状态

# 代码生成
make proto-gen          # Proto代码生成
make proto-lint         # Proto格式检查

# 测试
make load-test          # 运行压测
make load-test SCENARIO=api  # 指定场景

# 发布
make release            # 创建发布
make release VERSION=1.0.0   # 指定版本
```

---

## 二、脚本参数规范

```bash
# 统一参数格式
./tools/dev-env/setup.sh \
  --components=java,python,postgres \
  --skip-db \
  --verbose

# 环境变量配置
export SOLRA_ENV=dev
export SOLRA_DB_HOST=localhost
```

---

## 三、输出格式

| 输出类型 | 格式 |
|---------|------|
| 进度 | `[N/M] 描述... ✓/✗` |
| 错误 | `Error: 原因\n  解决方法: ...` |
| 结果 | JSON (可被其他工具解析) |
| 帮助 | Markdown格式 |
