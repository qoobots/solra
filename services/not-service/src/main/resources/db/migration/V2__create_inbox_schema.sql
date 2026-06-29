-- ============================================================
-- not-service 数据库 Schema 迁移 V2
-- NOT-002 应用内消息中心
-- ============================================================

-- 收件箱消息表
CREATE TABLE IF NOT EXISTS inbox_messages (
    message_id      VARCHAR(64)     PRIMARY KEY,
    sender_id       VARCHAR(64),
    recipient_id    VARCHAR(64)     NOT NULL,
    type            VARCHAR(20)     NOT NULL DEFAULT 'TEXT',
    status          VARCHAR(20)     NOT NULL DEFAULT 'SENT',
    title           VARCHAR(256),
    content         VARCHAR(4096),
    attachment_url  VARCHAR(1024),
    metadata        VARCHAR(1024),
    conversation_id VARCHAR(64),
    sent_at         TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_im_recipient ON inbox_messages(recipient_id, sent_at DESC);
CREATE INDEX idx_im_recipient_status ON inbox_messages(recipient_id, status);
CREATE INDEX idx_im_conversation ON inbox_messages(conversation_id, sent_at DESC);
CREATE INDEX idx_im_sender_recipient ON inbox_messages(sender_id, recipient_id);
