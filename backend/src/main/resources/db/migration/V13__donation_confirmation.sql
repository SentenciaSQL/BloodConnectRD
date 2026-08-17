SET TIME ZONE 'America/Santo_Domingo';

ALTER TABLE donations
    ALTER COLUMN status TYPE VARCHAR(32);

ALTER TABLE donations
    DROP CONSTRAINT IF EXISTS donations_status_check;

ALTER TABLE donations
    ADD COLUMN IF NOT EXISTS confirmed_units INTEGER NOT NULL DEFAULT 0;

UPDATE donations
SET confirmed_units = units,
    status = 'CONFIRMED'
WHERE status = 'COMPLETED';

ALTER TABLE donations
    ADD CONSTRAINT donations_status_check
        CHECK (status IN ('REPORTED', 'PARTIALLY_CONFIRMED', 'CONFIRMED', 'CANCELLED'));

ALTER TABLE donations
    DROP CONSTRAINT IF EXISTS donations_confirmed_units_range;

ALTER TABLE donations
    ADD CONSTRAINT donations_confirmed_units_range
        CHECK (confirmed_units >= 0 AND confirmed_units <= units);

-- Donación reportada de demostración, pendiente de confirmación del receptor.
INSERT INTO donations (
    donor_id, blood_request_id, donation_center_id, donation_date, units, confirmed_units, notes, status
)
SELECT
    d.id,
    br.id,
    NULL,
    CURRENT_DATE,
    2,
    0,
    'Donación reportada de demostración, pendiente de confirmación',
    'REPORTED'
FROM donors d
JOIN users u ON u.id = d.user_id
JOIN blood_requests br ON br.patient_name = 'Paciente Demo Uno'
WHERE u.email = 'ana.donor@bloodconnect.do';
