SET TIME ZONE 'America/Santo_Domingo';

CREATE TABLE donation_centers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('HOSPITAL', 'CLINIC', 'BLOOD_BANK', 'MEDICAL_CENTER', 'OTHER')),
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    municipality_id BIGINT NOT NULL REFERENCES municipalities(id),
    sector VARCHAR(120),
    address VARCHAR(255) NOT NULL,
    reference VARCHAR(255),
    phone VARCHAR(20),
    schedule VARCHAR(255),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
