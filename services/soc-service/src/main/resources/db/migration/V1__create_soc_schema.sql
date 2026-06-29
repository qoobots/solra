-- ============================================================
-- soc-service 数据库 Schema 迁移 V1
-- SOC-004 好友系统 + SOC-005 空间分享裂变
-- ============================================================

-- 分享会话表
CREATE TABLE IF NOT EXISTS share_sessions (
    share_id        VARCHAR(64)     PRIMARY KEY,
    space_id        VARCHAR(64)     NOT NULL,
    sharer_user_id  VARCHAR(64)     NOT NULL,
    share_type      VARCHAR(20)     NOT NULL,
    share_code      VARCHAR(32)     NOT NULL UNIQUE,
    click_count     BIGINT          NOT NULL DEFAULT 0,
    conversion_count BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    viral_chain     TEXT,           -- JSON array
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_share_sessions_code ON share_sessions(share_code);
CREATE INDEX idx_share_sessions_sharer ON share_sessions(sharer_user_id);
CREATE INDEX idx_share_sessions_space ON share_sessions(space_id);

-- 分享点击记录表
CREATE TABLE IF NOT EXISTS share_clicks (
    click_id        VARCHAR(64)     PRIMARY KEY,
    share_id        VARCHAR(64)     NOT NULL REFERENCES share_sessions(share_id),
    visitor_user_id VARCHAR(64),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    platform        VARCHAR(20),
    timestamp       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    converted       BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_share_clicks_share ON share_clicks(share_id);

-- 好友关系表
CREATE TABLE IF NOT EXISTS friends (
    friendship_id   VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL,
    friend_user_id  VARCHAR(64)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMPTZ,
    UNIQUE(user_id, friend_user_id)
);

CREATE INDEX idx_friends_user ON friends(user_id);
CREATE INDEX idx_friends_friend ON friends(friend_user_id);
