SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE donations (
    id BIGSERIAL PRIMARY KEY,
    donor_id BIGINT NOT NULL REFERENCES donors(id),
    blood_request_id BIGINT REFERENCES blood_requests(id),
    donation_center_id BIGINT REFERENCES donation_centers(id),
    donation_date DATE NOT NULL,
    units INTEGER NOT NULL CHECK (units > 0),
    notes VARCHAR(500),
    status VARCHAR(20) NOT NULL CHECK (status IN ('COMPLETED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
