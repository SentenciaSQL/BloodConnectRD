SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE blood_requests (
    id BIGSERIAL PRIMARY KEY,
    created_by BIGINT NOT NULL REFERENCES users(id),
    patient_name VARCHAR(200) NOT NULL,
    blood_type VARCHAR(3) NOT NULL CHECK (blood_type IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    units_required INTEGER NOT NULL CHECK (units_required > 0),
    hospital VARCHAR(200) NOT NULL,
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    municipality_id BIGINT NOT NULL REFERENCES municipalities(id),
    sector VARCHAR(120),
    address VARCHAR(255) NOT NULL,
    reference VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    deadline TIMESTAMPTZ NOT NULL,
    description TEXT,
    contact_phone VARCHAR(20) NOT NULL,
    urgency VARCHAR(10) NOT NULL CHECK (urgency IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'FULFILLED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
