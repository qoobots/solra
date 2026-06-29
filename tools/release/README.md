# Solra Release Tool

## 多服务版本发布脚本

统一管理 15 个微服务的版本发布流程。

### 使用方法

```bash
# 预览发布 (dry-run)
python tools/release/release.py --dry-run --bump patch

# 发布指定服务
python tools/release/release.py --services auth-service,avt-service,spc-service --bump minor

# 发布所有服务到指定版本
python tools/release/release.py --version 0.3.0

# 跳过构建 (仅 changelog + tag)
python tools/release/release.py --skip-build --bump patch
```

### 发布流程

1. 读取当前版本（build.gradle.kts / pyproject.toml）
2. 根据 bump 类型计算新版本
3. 从 git log 生成 changelog → `01docs/changelogs/`
4. 构建服务制品 (Gradle/Poetry)
5. 创建 git tag `<service>/v<version>`
6. 推送到远程: `git push --tags`
