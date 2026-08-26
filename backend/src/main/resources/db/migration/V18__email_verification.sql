-- Los usuarios existentes quedan verificados porque
-- inicialmente la columna se crea con DEFAULT TRUE.
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Los registros nuevos quedarán sin verificar.
ALTER TABLE users
    ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE TABLE email_verification_tokens (
                                           id BIGSERIAL PRIMARY KEY,

                                           user_id BIGINT NOT NULL
                                               REFERENCES users(id)
                                                   ON DELETE CASCADE,

                                           token_hash VARCHAR(64) NOT NULL UNIQUE,

                                           expires_at TIMESTAMPTZ NOT NULL,

                                           used_at TIMESTAMPTZ,

                                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_verification_tokens_user_id
    ON email_verification_tokens(user_id);

CREATE INDEX idx_email_verification_tokens_expires_at
    ON email_verification_tokens(expires_at);
