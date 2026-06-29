-- ============================================================
-- not-service 数据库 Schema 迁移 V1
-- NOT-001 推送通知系统
-- ============================================================

-- 通知表
CREATE TABLE IF NOT EXISTS notifications (
    notification_id VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    priority        VARCHAR(10)     NOT NULL DEFAULT 'NORMAL',
    title           VARCHAR(200)    NOT NULL,
    body            VARCHAR(2000)   NOT NULL DEFAULT '',
    image_url       VARCHAR(500),
    deep_link       VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    metadata        TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    read_at         TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ
);

CREATE INDEX idx_notif_user ON notifications(user_id);
CREATE INDEX idx_notif_user_status ON notifications(user_id, status);
CREATE INDEX idx_notif_user_created ON notifications(user_id, created_at DESC);

-- 推送消息表
CREATE TABLE IF NOT EXISTS push_messages (
    push_id             VARCHAR(64)     PRIMARY KEY,
    notification_id     VARCHAR(64)     NOT NULL REFERENCES notifications(notification_id),
    device_token        VARCHAR(512)    NOT NULL,
    platform            VARCHAR(10)     NOT NULL,
    push_provider       VARCHAR(10)     NOT NULL,
    provider_message_id VARCHAR(256),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    sent_at             TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    failure_reason      VARCHAR(256)
);

CREATE INDEX idx_push_notif ON push_messages(notification_id);

-- 设备注册表
CREATE TABLE IF NOT EXISTS device_registrations (
    registration_id VARCHAR(64)     PRIMARY KEY,
    user_id         VARCHAR(64)     NOT NULL,
    device_token    VARCHAR(512)    NOT NULL UNIQUE,
    platform        VARCHAR(10)     NOT NULL,
    push_provider   VARCHAR(10)     NOT NULL,
    device_name     VARCHAR(100),
    app_version     VARCHAR(20),
    os_version      VARCHAR(20),
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_used_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dev_user ON device_registrations(user_id);
CREATE INDEX idx_dev_token ON device_registrations(device_token);

-- 通知偏好表
CREATE TABLE IF NOT EXISTS notification_preferences (
    pref_id             VARCHAR(64)     PRIMARY KEY,
    user_id             VARCHAR(64)     NOT NULL,
    notification_type   VARCHAR(30)     NOT NULL,
    channel             VARCHAR(10)     NOT NULL DEFAULT 'PUSH',
    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    quiet_hours_start   VARCHAR(5),
    quiet_hours_end     VARCHAR(5),
    UNIQUE(user_id, notification_type, channel)
);

CREATE INDEX idx_pref_user ON notification_preferences(user_id);
