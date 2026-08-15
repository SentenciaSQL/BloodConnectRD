SET TIME ZONE 'America/Santo_Domingo';

INSERT INTO donation_responses (blood_request_id, donor_id, status, message) VALUES
    (
        (SELECT id FROM blood_requests WHERE patient_name = 'Paciente Demo Uno'),
        (SELECT id FROM donors WHERE user_id = (SELECT id FROM users WHERE email = 'miguel.donor@bloodconnect.do')),
        'PENDING',
        'Puedo acudir hoy en la tarde.'
    ),
    (
        (SELECT id FROM blood_requests WHERE patient_name = 'Paciente Demo Dos'),
        (SELECT id FROM donors WHERE user_id = (SELECT id FROM users WHERE email = 'ana.donor@bloodconnect.do')),
        'ACCEPTED',
        'Estoy disponible mañana por la mañana.'
    ),
    (
        (SELECT id FROM blood_requests WHERE patient_name = 'Paciente Demo Histórico'),
        (SELECT id FROM donors WHERE user_id = (SELECT id FROM users WHERE email = 'sofia.donor@bloodconnect.do')),
        'COMPLETED',
        'Donación histórica de demostración'
    );
