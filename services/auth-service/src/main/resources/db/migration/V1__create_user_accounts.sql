-- Flyway migration V1: Create user_accounts table
CREATE TABLE IF NOT EXISTS user_accounts (
    user_id             VARCHAR(64)  PRIMARY KEY,
    username            VARCHAR(64)  UNIQUE,
    display_name        VARCHAR(128) NOT NULL,
    email               VARCHAR(256),
    phone               VARCHAR(20)  UNIQUE,
    avatar_url          VARCHAR(512),
    password_hash       VARCHAR(256),
    status              VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    real_name           VARCHAR(64),
    id_number_hash      VARCHAR(256),
    birth_date          VARCHAR(10),
    real_name_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    real_name_verified_at TIMESTAMPTZ,
    roles               VARCHAR(512) NOT NULL DEFAULT 'ROLE_USER',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_login_at       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_username ON user_accounts(username);
CREATE INDEX IF NOT EXISTS idx_phone ON user_accounts(phone);
CREATE INDEX IF NOT EXISTS idx_email ON user_accounts(email);
CREATE INDEX IF NOT EXISTS idx_status ON user_accounts(status);
