SET TIME ZONE 'America/Santo_Domingo';

-- Datos exclusivamente ficticios para demostración. Las contraseñas usan BCrypt con costo 10.
INSERT INTO users (first_name, last_name, email, password, phone, role, enabled) VALUES
    ('Admin', 'BloodConnect Demo', 'admin@bloodconnect.do', '$2b$10$O/eWtvVkFSBPrVJL8F2XE.s/8zJqpSZeByMgu9e6JSZkJZZYsCKQC', '+18095550000', 'ADMIN', TRUE),
    ('Ana', 'Pérez Demo', 'ana.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550101', 'DONOR', TRUE),
    ('Luis', 'Gómez Demo', 'luis.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550102', 'DONOR', TRUE),
    ('María', 'Santos Demo', 'maria.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550103', 'DONOR', TRUE),
    ('José', 'Ramírez Demo', 'jose.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550104', 'DONOR', TRUE),
    ('Carla', 'Méndez Demo', 'carla.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550105', 'DONOR', TRUE),
    ('Pedro', 'Rosa Demo', 'pedro.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550106', 'DONOR', TRUE),
    ('Sofía', 'Castillo Demo', 'sofia.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550107', 'DONOR', TRUE),
    ('Miguel', 'Torres Demo', 'miguel.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550108', 'DONOR', TRUE),
    ('Elena', 'Vargas Demo', 'elena.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550109', 'DONOR', TRUE),
    ('Raúl', 'Díaz Demo', 'raul.donor@bloodconnect.do', '$2b$10$AGmx2FOtRXZsFOlYWdLwlOZhTDapkdB/v3A8zkk/m/IUQVjVDYu4a', '+18095550110', 'DONOR', TRUE),
    ('Laura', 'Solicitante Demo', 'laura.user@bloodconnect.do', '$2b$10$1B8ljDU1uxenZgV/4HlPHugvyzjs1T2NvM4SsPXErr82PuDRJ4uA.', '+18095550201', 'USER', TRUE),
    ('Carlos', 'Solicitante Demo', 'carlos.user@bloodconnect.do', '$2b$10$1B8ljDU1uxenZgV/4HlPHugvyzjs1T2NvM4SsPXErr82PuDRJ4uA.', '+18095550202', 'USER', TRUE);

INSERT INTO donors (
    user_id, blood_type, birth_date, sex, phone, province_id, municipality_id,
    sector, approximate_address, latitude, longitude, last_donation_date, availability
) VALUES
    ((SELECT id FROM users WHERE email='ana.donor@bloodconnect.do'), 'A+', '1992-04-12', 'FEMALE', '+18095550101', 1, (SELECT id FROM municipalities WHERE code='DN-01'), 'Naco', 'Zona central', 18.4732000, -69.9385000, CURRENT_DATE - 90, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='luis.donor@bloodconnect.do'), 'A-', '1988-09-03', 'MALE', '+18095550102', 2, (SELECT id FROM municipalities WHERE code='SD-01'), 'Alma Rosa', 'Zona oriental', 18.4855000, -69.8583000, NULL, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='maria.donor@bloodconnect.do'), 'B+', '1995-01-20', 'FEMALE', '+18095550103', 2, (SELECT id FROM municipalities WHERE code='SD-02'), 'Villa Mella', 'Zona norte', 18.5487000, -69.9040000, CURRENT_DATE - 120, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='jose.donor@bloodconnect.do'), 'B-', '1985-07-14', 'MALE', '+18095550104', 2, (SELECT id FROM municipalities WHERE code='SD-03'), 'Herrera', 'Zona oeste', 18.4728000, -70.0101000, CURRENT_DATE - 45, 'TEMPORARILY_UNAVAILABLE'),
    ((SELECT id FROM users WHERE email='carla.donor@bloodconnect.do'), 'AB+', '1990-11-08', 'FEMALE', '+18095550105', 3, (SELECT id FROM municipalities WHERE code='STG-01'), 'Los Jardines', 'Santiago centro', 19.4517000, -70.6970000, CURRENT_DATE - 100, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='pedro.donor@bloodconnect.do'), 'AB-', '1987-05-19', 'MALE', '+18095550106', 4, (SELECT id FROM municipalities WHERE code='VEG-01'), 'Villa Lora', 'La Vega centro', 19.2221000, -70.5299000, NULL, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='sofia.donor@bloodconnect.do'), 'O+', '1998-03-25', 'FEMALE', '+18095550107', 5, (SELECT id FROM municipalities WHERE code='SC-01'), 'Madre Vieja', 'San Cristóbal centro', 18.4167000, -70.1000000, CURRENT_DATE - 70, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='miguel.donor@bloodconnect.do'), 'O-', '1989-12-01', 'MALE', '+18095550108', 6, (SELECT id FROM municipalities WHERE code='PP-01'), 'Padre Las Casas', 'Puerto Plata centro', 19.7934000, -70.6884000, CURRENT_DATE - 130, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='elena.donor@bloodconnect.do'), 'A+', '1994-06-30', 'FEMALE', '+18095550109', 8, (SELECT id FROM municipalities WHERE code='LR-01'), 'Buena Vista', 'La Romana centro', 18.4273000, -68.9728000, NULL, 'AVAILABLE'),
    ((SELECT id FROM users WHERE email='raul.donor@bloodconnect.do'), 'O+', '1991-10-16', 'MALE', '+18095550110', 10, (SELECT id FROM municipalities WHERE code='SPM-01'), 'Miramar', 'San Pedro centro', 18.4539000, -69.3086000, CURRENT_DATE - 80, 'AVAILABLE');

INSERT INTO blood_requests (
    created_by, patient_name, blood_type, units_required, hospital, province_id, municipality_id,
    sector, address, reference, latitude, longitude, deadline, description, contact_phone, urgency, status
) VALUES
    ((SELECT id FROM users WHERE email='laura.user@bloodconnect.do'), 'Paciente Demo Uno', 'O-', 3, 'Hospital Central Demo', 1, (SELECT id FROM municipalities WHERE code='DN-01'), 'Gazcue', 'Av. Demo 100', 'Emergencias, caso ficticio', 18.4720000, -69.8890000, NOW() + INTERVAL '18 hours', 'Solicitud crítica de demostración', '+18095550201', 'CRITICAL', 'OPEN'),
    ((SELECT id FROM users WHERE email='carlos.user@bloodconnect.do'), 'Paciente Demo Dos', 'A+', 2, 'Clínica Oriental Demo', 2, (SELECT id FROM municipalities WHERE code='SD-01'), 'Los Mina', 'Calle Demo 20', 'Recepción principal', 18.4970000, -69.8610000, NOW() + INTERVAL '3 days', 'Solicitud urgente de demostración', '+18095550202', 'HIGH', 'OPEN'),
    ((SELECT id FROM users WHERE email='laura.user@bloodconnect.do'), 'Paciente Demo Tres', 'B+', 1, 'Centro Médico Cibao Demo', 3, (SELECT id FROM municipalities WHERE code='STG-01'), 'Centro', 'Av. Cibao Demo 45', 'Segundo nivel', 19.4500000, -70.6900000, NOW() + INTERVAL '7 days', 'Solicitud regular de demostración', '+18095550201', 'MEDIUM', 'OPEN'),
    ((SELECT id FROM users WHERE email='carlos.user@bloodconnect.do'), 'Paciente Demo Histórico', 'O+', 1, 'Hospital Sur Demo', 5, (SELECT id FROM municipalities WHERE code='SC-01'), 'Centro', 'Calle Histórica Demo 8', NULL, 18.4200000, -70.1050000, NOW() - INTERVAL '30 days', 'Solicitud completada de demostración', '+18095550202', 'LOW', 'FULFILLED');

INSERT INTO donation_centers (
    name, type, province_id, municipality_id, sector, address, reference, phone, schedule, latitude, longitude, active
) VALUES
    ('Hospital Central Demo DN', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE code='DN-01'), 'Gazcue', 'Av. Demo 100', 'Edificio ficticio', '+18095551001', 'Lun-Vie 8:00 AM - 5:00 PM', 18.4720000, -69.8890000, TRUE),
    ('Banco de Sangre Oriental Demo', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE code='SD-01'), 'Alma Rosa', 'Calle Demo 25', 'Local ficticio', '+18095551002', 'Todos los días 7:00 AM - 7:00 PM', 18.4860000, -69.8580000, TRUE),
    ('Centro Médico Cibao Demo', 'MEDICAL_CENTER', 3, (SELECT id FROM municipalities WHERE code='STG-01'), 'Los Jardines', 'Av. Cibao Demo 45', 'Centro ficticio', '+18095551003', 'Lun-Sáb 8:00 AM - 4:00 PM', 19.4510000, -70.6960000, TRUE),
    ('Clínica La Vega Demo', 'CLINIC', 4, (SELECT id FROM municipalities WHERE code='VEG-01'), 'Centro', 'Calle Vega Demo 9', 'Clínica ficticia', '+18095551004', 'Lun-Vie 9:00 AM - 4:00 PM', 19.2210000, -70.5300000, TRUE),
    ('Banco Costero Demo', 'BLOOD_BANK', 6, (SELECT id FROM municipalities WHERE code='PP-01'), 'Centro', 'Malecón Demo 12', 'Centro ficticio', '+18095551005', 'Lun-Sáb 8:00 AM - 5:00 PM', 19.7900000, -70.6900000, TRUE);

INSERT INTO donations (donor_id, blood_request_id, donation_center_id, donation_date, units, notes, status) VALUES
    ((SELECT id FROM donors WHERE user_id=(SELECT id FROM users WHERE email='ana.donor@bloodconnect.do')), NULL, (SELECT id FROM donation_centers WHERE name='Hospital Central Demo DN'), CURRENT_DATE - 90, 1, 'Donación voluntaria demo', 'COMPLETED'),
    ((SELECT id FROM donors WHERE user_id=(SELECT id FROM users WHERE email='maria.donor@bloodconnect.do')), NULL, (SELECT id FROM donation_centers WHERE name='Banco de Sangre Oriental Demo'), CURRENT_DATE - 120, 1, 'Donación voluntaria demo', 'COMPLETED'),
    ((SELECT id FROM donors WHERE user_id=(SELECT id FROM users WHERE email='sofia.donor@bloodconnect.do')), (SELECT id FROM blood_requests WHERE patient_name='Paciente Demo Histórico'), NULL, CURRENT_DATE - 70, 1, 'Respuesta histórica demo', 'COMPLETED'),
    ((SELECT id FROM donors WHERE user_id=(SELECT id FROM users WHERE email='miguel.donor@bloodconnect.do')), NULL, (SELECT id FROM donation_centers WHERE name='Banco Costero Demo'), CURRENT_DATE - 130, 1, 'Donación voluntaria demo', 'COMPLETED');

INSERT INTO notifications (user_id, type, title, message, resource_type, resource_id, read) VALUES
    ((SELECT id FROM users WHERE email='laura.user@bloodconnect.do'), 'SYSTEM', 'Bienvenida a BloodConnect RD', 'Estos son datos ficticios de demostración.', NULL, NULL, FALSE),
    ((SELECT id FROM users WHERE email='ana.donor@bloodconnect.do'), 'DONATION_REMINDER', 'Gracias por donar', 'Tu historial demo está disponible.', 'DONATION', (SELECT MIN(id) FROM donations), FALSE),
    ((SELECT id FROM users WHERE email='carlos.user@bloodconnect.do'), 'REQUEST_FULFILLED', 'Solicitud completada', 'La solicitud histórica demo fue completada.', 'BLOOD_REQUEST', (SELECT id FROM blood_requests WHERE patient_name='Paciente Demo Histórico'), TRUE);
