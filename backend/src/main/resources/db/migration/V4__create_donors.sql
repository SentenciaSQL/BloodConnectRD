SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE donors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    blood_type VARCHAR(3) NOT NULL CHECK (blood_type IN ('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-')),
    birth_date DATE NOT NULL,
    sex VARCHAR(10) NOT NULL CHECK (sex IN ('MALE', 'FEMALE', 'OTHER')),
    phone VARCHAR(20) NOT NULL,
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    municipality_id BIGINT NOT NULL REFERENCES municipalities(id),
    sector VARCHAR(120),
    approximate_address VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    last_donation_date DATE,
    availability VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (availability IN ('AVAILABLE', 'TEMPORARILY_UNAVAILABLE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
