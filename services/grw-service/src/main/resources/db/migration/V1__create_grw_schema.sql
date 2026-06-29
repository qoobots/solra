-- ============================================================
-- grw-service 数据库 Schema 迁移 V1
-- GRW-002 决定性时刻 + GRW-006 新用户引导
-- ============================================================

-- 用户画像表
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id                 VARCHAR(64)     PRIMARY KEY,
    presence_score          DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    faith_level             VARCHAR(20)     NOT NULL DEFAULT 'SEEKER',
    total_interactions      INT             NOT NULL DEFAULT 0,
    spaces_visited          INT             NOT NULL DEFAULT 0,
    conversations_had       INT             NOT NULL DEFAULT 0,
    friends_count           INT             NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_active_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    current_onboarding_step VARCHAR(50),
    onboarding_completed    BOOLEAN         NOT NULL DEFAULT FALSE
);

-- 决定性时刻表
CREATE TABLE IF NOT EXISTS decisive_moments (
    moment_id       VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL,
    moment_type     VARCHAR(50)     NOT NULL,
    detected_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    state_before    TEXT,
    state_after     TEXT,
    conversion_value DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    triggered       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_dm_user ON decisive_moments(user_id);
CREATE INDEX idx_dm_user_type ON decisive_moments(user_id, moment_type);

-- 引导路径表
CREATE TABLE IF NOT EXISTS onboarding_paths (
    path_id         VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL UNIQUE,
    current_step    INT             NOT NULL DEFAULT 0,
    total_steps     INT             NOT NULL DEFAULT 6,
    step_history    TEXT,
    start_time      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'NOT_STARTED'
);

-- 经验事件表
CREATE TABLE IF NOT EXISTS experience_events (
    event_id        VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL,
    event_type      VARCHAR(50)     NOT NULL,
    space_id        VARCHAR(64),
    value           INT             NOT NULL DEFAULT 0,
    metadata        VARCHAR(512),
    timestamp       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ee_user ON experience_events(user_id);
CREATE INDEX idx_ee_user_ts ON experience_events(user_id, timestamp);
CREATE INDEX idx_ee_user_type ON experience_events(user_id, event_type);
