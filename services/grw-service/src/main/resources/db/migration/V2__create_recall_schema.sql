-- ============================================================
-- grw-service 数据库 Schema 迁移 V2
-- GRW-007 用户召回推送策略
-- ============================================================

-- 召回策略表
CREATE TABLE IF NOT EXISTS recall_strategies (
    strategy_id         VARCHAR(64)     PRIMARY KEY,
    name                VARCHAR(128)    NOT NULL,
    target_risk_level   VARCHAR(20)     NOT NULL,
    inactive_days_min   INT             NOT NULL DEFAULT 0,
    inactive_days_max   INT             NOT NULL DEFAULT 999,
    message_template    VARCHAR(1024),
    title_template      VARCHAR(256),
    channels            VARCHAR(256)    NOT NULL DEFAULT 'PUSH',
    max_attempts        INT             NOT NULL DEFAULT 3,
    cooldown_hours      INT             NOT NULL DEFAULT 72,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rs_risk_level ON recall_strategies(target_risk_level);
CREATE INDEX idx_rs_active ON recall_strategies(active);

-- 召回任务表
CREATE TABLE IF NOT EXISTS recall_tasks (
    task_id             VARCHAR(64)     PRIMARY KEY,
    user_id             VARCHAR(64)     NOT NULL,
    strategy_id         VARCHAR(64),
    strategy_name       VARCHAR(128),
    risk_level          VARCHAR(20),
    inactive_days       INT             NOT NULL DEFAULT 0,
    channel             VARCHAR(20)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    title               VARCHAR(256),
    message             VARCHAR(1024),
    attempt_number      INT             NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    sent_at             TIMESTAMPTZ,
    clicked_at          TIMESTAMPTZ,
    converted_at        TIMESTAMPTZ
);

CREATE INDEX idx_rt_user ON recall_tasks(user_id);
CREATE INDEX idx_rt_user_status ON recall_tasks(user_id, status);
CREATE INDEX idx_rt_status ON recall_tasks(status);
CREATE INDEX idx_rt_user_created ON recall_tasks(user_id, created_at);

-- 扩展用户画像表：增加召回相关字段
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS last_recall_at TIMESTAMPTZ;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS recall_count INT NOT NULL DEFAULT 0;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS churn_risk_level VARCHAR(20);
