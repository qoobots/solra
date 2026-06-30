# 13_数据库初始化SQL脚本说明

> **文档编号**：03-13
> **版本号**：v1.0
> **编制人**：数据组
> **审核人**：技术委员会
> **状态**：已发布
> **更新日期**：2026-06-30

---

## 1. 脚本目录结构

```
contracts/database/
├── migrations/               # Flyway迁移脚本
│   ├── auth/
│   │   ├── V1__init_auth_schema.sql
│   │   ├── V2__add_oauth_tables.sql
│   │   └── V3__seed_roles.sql
│   ├── avt/
│   │   ├── V1__init_avt_schema.sql
│   │   └── V2__add_message_partitioning.sql
│   ├── spc/
│   │   ├── V1__init_spc_schema.sql
│   │   └── V2__add_space_indexes.sql
│   ├── soc/
│   │   ├── V1__init_soc_schema.sql
│   │   └── V2__add_message_partitioning.sql
│   ├── mon/
│   │   └── V1__init_mon_schema.sql
│   ├── saf/
│   │   └── V1__init_saf_schema.sql
│   ├── grw/
│   │   └── V1__init_grw_schema.sql
│   ├── not/
│   │   └── V1__init_not_schema.sql
│   └── inf/
│       └── V1__init_inf_schema.sql
├── seeds/                    # 种子数据
│   ├── roles.csv             # 初始角色数据
│   ├── categories.csv        # 空间分类
│   └── system_configs.csv    # 系统配置
├── partitioning/             # 分区管理脚本
│   ├── create_partitions.sql
│   └── archive_partitions.sql
└── init-databases.sql        # 创建数据库脚本
```

---

## 2. 数据库初始化

### 2.1 创建数据库

```sql
-- init-databases.sql
-- 以超级用户执行

-- 创建应用用户
CREATE USER solra_app WITH PASSWORD '${APP_PASSWORD}' CREATEDB;
CREATE USER solra_migration WITH PASSWORD '${MIGRATION_PASSWORD}';

-- 创建各微服务数据库
CREATE DATABASE auth_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE avt_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE spc_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE crt_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE soc_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE grw_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE mon_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE saf_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE not_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';
CREATE DATABASE inf_db OWNER solra_app ENCODING 'UTF8' LC_COLLATE 'C' LC_CTYPE 'C';

-- 配置默认权限
ALTER DEFAULT PRIVILEGES FOR USER solra_app GRANT ALL ON TABLES TO solra_app;
ALTER DEFAULT PRIVILEGES FOR USER solra_app GRANT USAGE ON SEQUENCES TO solra_app;
```

### 2.2 数据库配置

```sql
-- 每个数据库执行
ALTER SYSTEM SET timezone = 'UTC';
ALTER SYSTEM SET default_text_search_config = 'pg_catalog.simple';

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";      -- UUID生成
CREATE EXTENSION IF NOT EXISTS "pgcrypto";        -- 加密函数
CREATE EXTENSION IF NOT EXISTS "pg_trgm";         -- 模糊搜索
CREATE EXTENSION IF NOT EXISTS "pg_partman";      -- 分区管理
CREATE EXTENSION IF NOT EXISTS "btree_gin";       -- GIN索引
```

---

## 3. 核心建表脚本示例

### 3.1 auth_db V1__init_auth_schema.sql

```sql
-- 启用UUID v7生成函数
CREATE OR REPLACE FUNCTION uuid_v7() RETURNS UUID AS $$
DECLARE
    timestamp_ms BIGINT;
    random_bytes BYTEA;
BEGIN
    timestamp_ms := (EXTRACT(EPOCH FROM clock_timestamp()) * 1000)::BIGINT;
    random_bytes := gen_random_bytes(10);
    RETURN encode(
        set_bit(set_bit(
            substring(int8send(timestamp_ms) FROM 1 FOR 6)::BYTEA,
            52, 0), 53, 1) || substring(random_bytes FROM 2 FOR 10),
        'hex'
    )::UUID;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 自动更新 updated_at 触发器函数
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 用户表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500),
    gender VARCHAR(10) CHECK (gender IN ('MALE','FEMALE','OTHER','SECRET')),
    birthday DATE,
    bio VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','INACTIVE','BANNED','DELETED')),
    real_name_verified BOOLEAN NOT NULL DEFAULT FALSE,
    youth_mode BOOLEAN NOT NULL DEFAULT FALSE,
    language VARCHAR(10) NOT NULL DEFAULT 'zh-CN',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ
);

-- 索引
CREATE INDEX idx_users_phone ON users(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_nickname ON users(nickname);

-- 触发器
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- 角色表
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    permissions TEXT[] NOT NULL DEFAULT '{}',
    priority INTEGER NOT NULL DEFAULT 0
);

-- 用户角色关联表
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_by UUID REFERENCES users(id),
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, role_id)
);

-- 会话表
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    user_id UUID NOT NULL REFERENCES users(id),
    access_token_hash VARCHAR(255) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_id VARCHAR(255),
    device_type VARCHAR(20),
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);
```

### 3.2 avt_db V1__init_avt_schema.sql

```sql
-- 虚拟人表
CREATE TABLE avatars (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    user_id UUID NOT NULL,
    name VARCHAR(50) NOT NULL,
    style VARCHAR(20) NOT NULL DEFAULT 'realistic'
        CHECK (style IN ('realistic','anime','cyberpunk','fantasy')),
    gender VARCHAR(10),
    face_params JSONB NOT NULL DEFAULT '{}',
    body_params JSONB NOT NULL DEFAULT '{}',
    skin_texture_url VARCHAR(500),
    model_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','ACTIVE','DELETED')),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version_number INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_avatars_user_id ON avatars(user_id);
CREATE INDEX idx_avatars_style ON avatars(style);

CREATE TRIGGER trg_avatars_updated_at
    BEFORE UPDATE ON avatars
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- 对话表
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_v7(),
    avatar_id UUID NOT NULL REFERENCES avatars(id),
    user_id UUID NOT NULL,
    title VARCHAR(200),
    context_summary TEXT,
    context_tokens INTEGER NOT NULL DEFAULT 0,
    message_count INTEGER NOT NULL DEFAULT 0,
    last_message_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','ARCHIVED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_conversations_user_avatar ON conversations(user_id, avatar_id);

-- 消息表（分区表）
CREATE TABLE messages (
    id UUID NOT NULL DEFAULT uuid_v7(),
    conversation_id UUID NOT NULL REFERENCES conversations(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant','system')),
    content TEXT NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'text',
    emotion VARCHAR(20),
    tokens_used INTEGER,
    model_used VARCHAR(50),
    latency_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- 创建初始分区
SELECT partman.create_parent(
    'public.messages',
    'created_at',
    'native',
    '1 month',
    p_premake := 3
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at);
```

---

## 4. 种子数据

### 4.1 roles.csv

```csv
id,name,description,permissions,priority
00000000-0000-0000-0000-000000000001,ROLE_USER,普通用户,"{space:enter,avatar:create,chat:send}",0
00000000-0000-0000-0000-000000000002,ROLE_CREATOR,创作者,"{space:create,asset:upload,template:publish}",10
00000000-0000-0000-0000-000000000003,ROLE_PREMIUM,高级会员,"{avatar:premium,space:premium,chat:unlimited}",5
00000000-0000-0000-0000-000000000004,ROLE_MODERATOR,审核员,"{content:review,report:handle,user:manage}",50
00000000-0000-0000-0000-000000000005,ROLE_ADMIN,管理员,"{*:*}",100
```

### 4.2 种子数据导入

```sql
-- V3__seed_roles.sql
INSERT INTO roles (id, name, description, permissions, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'ROLE_USER', '普通用户',
    ARRAY['space:enter','avatar:create','chat:send'], 0),
('00000000-0000-0000-0000-000000000002', 'ROLE_CREATOR', '创作者',
    ARRAY['space:create','asset:upload','template:publish'], 10)
ON CONFLICT (id) DO NOTHING;
```

---

## 5. 执行说明

### 5.1 本地开发环境

```bash
# Docker Compose 一键启动
docker-compose -f contracts/database/docker-compose.yml up -d

# 或手动执行
psql -h localhost -U solra_app -d auth_db -f migrations/auth/V1__init_auth_schema.sql
```

### 5.2 生产环境

Flyway在应用启动时自动执行迁移，无需手动操作。

```yaml
# application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

### 5.3 CI/CD集成

```yaml
# GitHub Actions 中验证迁移
- name: Validate Migrations
  run: |
    docker run --rm -v $PWD/contracts/database:/flyway/sql \
      flyway/flyway:latest \
      -url=jdbc:postgresql://postgres:5432/auth_db \
      -user=solra_app -password=${{ secrets.DB_PASSWORD }} \
      info
```

---

> **注**：完整SQL脚本存放于 `contracts/database/migrations/` 目录。
