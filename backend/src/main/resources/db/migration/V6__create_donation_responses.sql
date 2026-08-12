SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE donation_responses (
    id BIGSERIAL PRIMARY KEY,
    blood_request_id BIGINT NOT NULL REFERENCES blood_requests(id),
    donor_id BIGINT NOT NULL REFERENCES donors(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'CANCELLED')),
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_donation_responses_active
    ON donation_responses (blood_request_id, donor_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
