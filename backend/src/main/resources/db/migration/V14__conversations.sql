SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    blood_request_id BIGINT NOT NULL REFERENCES blood_requests(id),
    owner_user_id BIGINT NOT NULL REFERENCES users(id),
    donor_user_id BIGINT NOT NULL REFERENCES users(id),
    last_message_body VARCHAR(2000),
    last_message_at TIMESTAMPTZ,
    owner_last_read_at TIMESTAMPTZ,
    donor_last_read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_conversations_request_donor UNIQUE (blood_request_id, donor_user_id),
    CONSTRAINT chk_conversations_distinct_users CHECK (owner_user_id <> donor_user_id)
);

CREATE TABLE conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    body VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_owner_updated
    ON conversations (owner_user_id, last_message_at DESC NULLS LAST, created_at DESC);
CREATE INDEX idx_conversations_donor_updated
    ON conversations (donor_user_id, last_message_at DESC NULLS LAST, created_at DESC);
CREATE INDEX idx_conversation_messages_thread
    ON conversation_messages (conversation_id, created_at);
