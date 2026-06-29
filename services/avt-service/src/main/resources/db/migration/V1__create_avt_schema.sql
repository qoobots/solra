CREATE TABLE IF NOT EXISTS avt_conversations (
    conversation_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    space_id VARCHAR(64),
    avatar_id VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS avt_conversation_metadata (
    conversation_id VARCHAR(64) NOT NULL REFERENCES avt_conversations(conversation_id) ON DELETE CASCADE,
    meta_key VARCHAR(128) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (conversation_id, meta_key)
);

CREATE TABLE IF NOT EXISTS avt_dialogue_turns (
    turn_id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES avt_conversations(conversation_id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content VARCHAR(4096),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_turns_conv ON avt_dialogue_turns(conversation_id, timestamp);

CREATE TABLE IF NOT EXISTS avt_turn_chunks (
    turn_id VARCHAR(64) NOT NULL REFERENCES avt_dialogue_turns(turn_id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL,
    token TEXT NOT NULL,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (turn_id, sequence)
);

CREATE TABLE IF NOT EXISTS avt_turn_metadata (
    turn_id VARCHAR(64) NOT NULL REFERENCES avt_dialogue_turns(turn_id) ON DELETE CASCADE,
    meta_key VARCHAR(128) NOT NULL,
    meta_value TEXT,
    PRIMARY KEY (turn_id, meta_key)
);

CREATE TABLE IF NOT EXISTS avt_memories (
    memory_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    conversation_id VARCHAR(64),
    type VARCHAR(20) NOT NULL,
    content VARCHAR(2048),
    importance REAL NOT NULL DEFAULT 0.5,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_accessed TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_memories_user ON avt_memories(user_id, type, importance);
