# Solra Event Schema Version Management Strategy

> 本文档定义 Avro 事件 schema 的版本兼容策略，确保在事件基础设施演进过程中不破坏下游消费者。

## 1. 版本策略总览

| 策略 | 适用场景 | 示例 |
|------|---------|------|
| **Backward Compatible** (默认) | 新增可选字段、新增事件类型 | 下游可消费新版事件 |
| **Forward Compatible** | 新增必填字段（需默认值） | 上游可消费旧版数据 |
| **Full Compatible** | 兼有上述二者 | 生产者和消费者独立升级 |
| **Breaking Change** | 删除字段、修改类型、重命名 | 需主版本号升级 |

## 2. Schema 版本号规范

```
<namespace>.<event_name>:v<major>.<minor>
示例: solra.space.entered:v1.0
```

| 变更类型 | 版本变化 | 操作 |
|---------|---------|------|
| 新增可选字段 | `v1.0 → v1.1` (minor++) | 自动兼容 |
| 新增必填字段(有默认值) | `v1.1 → v1.2` (minor++) | 自动兼容 |
| 删除字段 | `v1.2 → v2.0` (major++) | 需协调升级 |
| 修改字段类型 | `v1.2 → v2.0` (major++) | 需协调升级 |
| 重命名字段 | `v1.2 → v2.0` (major++) | 需协调升级 |

## 3. 文件组织结构

```
contracts/events/avro/
├── v1/                          # 当前活跃版本
│   ├── space/                    # 空间域
│   │   ├── space_entered.avsc
│   │   ├── space_left.avsc
│   │   └── space_transition.avsc
│   ├── user/                     # 用户域
│   │   ├── user_profile_updated.avsc
│   │   └── user_faith_level_changed.avsc
│   ├── avatar/                   # 虚拟人域
│   │   ├── avatar_state_changed.avsc
│   │   └── avatar_interaction.avsc
│   ├── social/                   # 社交域
│   │   └── social_event.avsc
│   └── growth/                   # 成长域
│       └── achievement_unlocked.avsc
├── v2/                          # 下一版本(breaking changes)
├── compatibility/
│   ├── check.sh                 # Schema 兼容性检查脚本
│   └── migration/               # 迁移工具
│       └── generate_fallback.py # 从新schema生成旧版本兼容适配器
└── README.md
```

## 4. Schema 兼容性检查规则

```bash
# contracts/events/avro/compatibility/check.sh
#!/bin/bash
# Checks forward/backward compatibility between v1 and v2+
set -euo pipefail

OLD_VERSION="v1"
NEW_VERSION="v2"

for avsc in "$NEW_VERSION"/**/*.avsc; do
    event_name=$(basename "$avsc" .avsc)
    old_schema="$OLD_VERSION/**/$event_name.avsc"

    if [ -f "$old_schema" ]; then
        echo "Checking $event_name: $OLD_VERSION → $NEW_VERSION"
        
        # Backward: can readers using old schema read data written with new schema?
        java -jar avro-tools.jar compatibility "$avsc" "$old_schema" BACKWARD
        
        # Forward: can readers using new schema read data written with old schema?
        java -jar avro-tools.jar compatibility "$avsc" "$old_schema" FORWARD
    else
        echo "$event_name is new in $NEW_VERSION (no compatibility check needed)"
    fi
done
```

## 5. 兼容性变更示例

### 5.1 新增可选字段 (Backward Compatible)

```avro
// v1.0
{"name": "user_id", "type": "string"}

// v1.1 - OK: 新增可选字段
{"name": "user_id", "type": "string"},
{"name": "display_name", "type": ["null", "string"], "default": null}
```

### 5.2 新增必填字段（需默认值）(Forward Compatible)

```avro
// v1.2 - OK: 新增必填字段并指定默认值
{"name": "user_id", "type": "string"},
{"name": "display_name", "type": "string", "default": "Unknown User"}
```

### 5.3 删除字段 (Breaking → v2.0)

```avro
// v1.0 → v2.0: 删除 `legacy_field`
// v2.0 schema 中直接移除该字段
// 所有生产者和消费者必须同步升级
```

## 6. CI 集成

```yaml
# .github/workflows/avro-check.yml
name: Avro Schema Compatibility Check
on:
  pull_request:
    paths:
      - 'contracts/events/avro/**'
jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./contracts/events/avro/compatibility/check.sh
```

## 7. 迁移策略

当发生 breaking change 时：

1. **双写期**：生产者同时写入 v1 和 v2 版本事件
2. **消费者迁移**：消费者逐步从 v1 切换到 v2
3. **双读期**：消费者同时消费 v1（fallback）和 v2（primary）
4. **清理**：所有消费者迁移完毕后，停止 v1 生产

```
双写期 (2周) → 消费者迁移期 (2周) → 双读期 (1周) → 清理
```
