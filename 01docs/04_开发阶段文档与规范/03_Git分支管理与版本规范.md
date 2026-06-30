# 03_Git分支管理与版本规范

> **文档编号**：04-03
> **版本号**：v1.0
> **编制人**：DevOps组
> **审核人**：技术委员会
> **状态**：已发布
> **更新日期**：2026-06-30

---

## 1. 分支模型

采用 **Trunk-Based Development**（主干开发）模型。

### 1.1 分支类型

```
main (主干)
├── release/v1.0 (发布分支，临时)
├── hotfix/xxx (热修复分支，临时)
└── (开发者本地短分支，< 1天)
```

| 分支 | 生命周期 | 说明 |
|------|----------|------|
| `main` | 永久 | 主干，始终可发布 |
| `release/v*` | 临时（发布后删除） | 发布分支，仅用于发布前的最终修复 |
| `hotfix/*` | 临时（修复后删除） | 线上紧急修复 |
| 本地分支 | < 1天 | 开发者本地功能分支 |

### 1.2 工作流程

```
1. 从 main 拉最新代码
2. 本地创建短分支 feature/xxx
3. 开发 + 自测
4. 提交 PR → Code Review
5. CI通过后合并到 main
6. 删除本地分支

原则: 小步提交，频繁合并，分支存活 < 1天
```

---

## 2. 分支命名

| 类型 | 格式 | 示例 |
|------|------|------|
| 功能分支 | `feat/{module}-{description}` | `feat/avatar-ai-generation` |
| 修复分支 | `fix/{module}-{description}` | `fix/auth-token-expiry` |
| 热修复 | `hotfix/{version}-{description}` | `hotfix/v1.0.1-login-crash` |
| 发布分支 | `release/v{major}.{minor}` | `release/v1.0` |

---

## 3. Commit规范

### 3.1 Conventional Commits

```
<type>(<scope>): <description>

[body]

[footer]
```

### 3.2 Scope定义

| Scope | 说明 |
|-------|------|
| `auth` | 认证授权 |
| `avatar` | 虚拟人 |
| `space` | 空间 |
| `social` | 社交 |
| `store` | 商城 |
| `safety` | 内容安全 |
| `notify` | 通知 |
| `infra` | 基础设施 |
| `core` | 核心引擎 |
| `brain` | AI大脑 |
| `body` | 虚拟身体 |
| `web` | Web前端 |
| `mobile` | 移动端 |
| `desktop` | 桌面端 |
| `docs` | 文档 |
| `ci` | CI/CD |
| `deps` | 依赖 |

### 3.3 示例

```
feat(avatar): implement AI avatar generation from photo

- Add face detection using InsightFace
- Add FLAME parameter extraction
- Add GAN-based texture synthesis
- Add model generation pipeline

Closes #AVT-42
```

```
fix(auth): resolve token refresh race condition

When multiple requests trigger token refresh simultaneously,
only the first should execute the refresh, others should wait.

Fixes #AUTH-15
```

---

## 4. Pull Request规范

### 4.1 PR标题

```
{type}({scope}): {description}
```

### 4.2 PR模板

```markdown
## 变更描述
<!-- 简要描述此PR的变更内容 -->

## 变更类型
- [ ] 新功能 (feat)
- [ ] Bug修复 (fix)
- [ ] 重构 (refactor)
- [ ] 文档 (docs)
- [ ] 其他

## 测试
- [ ] 单元测试通过
- [ ] 集成测试通过
- [ ] 手动测试通过

## 检查清单
- [ ] 代码符合编码规范
- [ ] 已添加必要的测试
- [ ] 已更新相关文档
- [ ] 无安全风险
- [ ] 无性能退化

## 关联Issue
Closes #xxx
```

### 4.3 Code Review要求

| 要求 | 说明 |
|------|------|
| 至少1人审批 | 核心模块至少2人 |
| CI通过 | 所有检查必须绿色 |
| 无冲突 | 必须先rebase main |
| 无新增警告 | SonarQube无新增问题 |

---

## 5. 版本号规范

### 5.1 Semantic Versioning

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

示例:
1.0.0           # 正式版
1.0.1           # 补丁版本
1.1.0           # 次版本
2.0.0           # 主版本（破坏性变更）
1.1.0-alpha.1   # 预发布版
1.1.0-rc.1      # 候选版
```

### 5.2 版本递增规则

| 变更类型 | 版本递增 |
|----------|----------|
| Bug修复 | PATCH +1 |
| 新功能（向后兼容） | MINOR +1, PATCH = 0 |
| 破坏性变更 | MAJOR +1, MINOR = 0, PATCH = 0 |

### 5.3 版本号位置

| 文件 | 字段 |
|------|------|
| `build.gradle.kts` | `version = "1.0.0"` |
| `package.json` | `"version": "1.0.0"` |
| `pyproject.toml` | `version = "1.0.0"` |
| `CMakeLists.txt` | `project(solra VERSION 1.0.0)` |

---

## 6. Git Hooks

### 6.1 Pre-commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 检查代码格式
./gradlew spotlessCheck

# 检查暂存文件
STAGED_FILES=$(git diff --cached --name-only --diff-filter=ACM)

# Java检查
if echo "$STAGED_FILES" | grep -q '\.java$'; then
    ./gradlew checkstyleMain
fi

# TypeScript检查
if echo "$STAGED_FILES" | grep -q '\.ts$\|\.vue$'; then
    npx eslint --fix $STAGED_FILES
fi
```

### 6.2 Commit-msg Hook

```bash
#!/bin/bash
# .git/hooks/commit-msg

COMMIT_MSG=$(cat "$1")
PATTERN="^(feat|fix|docs|style|refactor|perf|test|chore|ci)(\([a-z-]+\))?: .+"

if ! echo "$COMMIT_MSG" | grep -qE "$PATTERN"; then
    echo "错误: Commit消息不符合Conventional Commits规范"
    echo "格式: type(scope): description"
    exit 1
fi
```

---

## 7. CI/CD流水线

### 7.1 分支触发

| 分支 | 触发条件 | 操作 |
|------|----------|------|
| `main` | Push / PR Merge | Build + Test + Deploy Staging |
| `main` | Tag `v*` | Build + Test + Deploy Prod (审批) |
| `release/*` | Push | Build + Test + Deploy Staging |
| PR | Open / Push | Build + Test + Lint |

### 7.2 质量门禁

| 门禁 | 阈值 |
|------|------|
| 编译通过 | 必须 |
| 单元测试通过率 | 100% |
| 代码覆盖率 | ≥80% (新增代码) |
| Checkstyle | 0错误 |
| SonarQube | 无新增Blocker/Critical |
| 安全扫描 | 无Critical漏洞 |

---

> **注**：GitHub Actions工作流文件存放于 `.github/workflows/` 目录。
