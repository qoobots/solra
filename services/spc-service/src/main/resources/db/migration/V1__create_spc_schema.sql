CREATE TABLE IF NOT EXISTS spc_spaces (
    space_id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(256) NOT NULL DEFAULT '',
    description VARCHAR(1024) DEFAULT '',
    category VARCHAR(32),
    thumbnail_url VARCHAR(512),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    privacy VARCHAR(20) DEFAULT 'PUBLIC',
    language_code VARCHAR(10) DEFAULT 'zh-CN',
    scene_file_url VARCHAR(512),
    entry_point VARCHAR(128),
    creator_id VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    share_count BIGINT NOT NULL DEFAULT 0,
    visitor_count BIGINT NOT NULL DEFAULT 0,
    conversation_count BIGINT NOT NULL DEFAULT 0,
    rating REAL NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS spc_space_tags (
    space_id VARCHAR(64) NOT NULL REFERENCES spc_spaces(space_id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (space_id, tag)
);

CREATE TABLE IF NOT EXISTS spc_space_metadata (
    space_id VARCHAR(64) NOT NULL REFERENCES spc_spaces(space_id) ON DELETE CASCADE,
    meta_key VARCHAR(128) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (space_id, meta_key)
);

CREATE INDEX idx_spaces_status ON spc_spaces(status);
CREATE INDEX idx_spaces_category ON spc_spaces(category, status);
CREATE INDEX idx_spaces_views ON spc_spaces(view_count DESC);

CREATE TABLE IF NOT EXISTS spc_user_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    space_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    dwell_duration_ms BIGINT DEFAULT 0,
    action_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_actions_user ON spc_user_actions(user_id, action_time DESC);
CREATE INDEX idx_actions_space ON spc_user_actions(space_id, action_time DESC);
