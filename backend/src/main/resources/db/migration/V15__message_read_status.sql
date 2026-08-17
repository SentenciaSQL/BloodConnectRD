SET TIME ZONE 'America/Santo_Domingo';

ALTER TABLE conversation_messages
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SENT'
        CHECK (status IN ('SENT', 'DELIVERED', 'READ')),
    ADD COLUMN delivered_at TIMESTAMPTZ,
    ADD COLUMN read_at TIMESTAMPTZ;

CREATE INDEX idx_conversation_messages_unread
    ON conversation_messages (conversation_id, sender_id, status);
