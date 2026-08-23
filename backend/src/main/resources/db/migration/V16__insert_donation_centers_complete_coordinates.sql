-- V16__insert_donation_centers.sql
-- BloodConnectRD
-- Seed nacional de bancos de sangre, hospitales, clínicas y centros médicos.
-- Los province_id y municipality.code corresponden al catálogo geográfico de BloodConnectRD.
-- Los campos no verificados con suficiente confianza se dejan en NULL.
-- Que un hospital figure aquí no implica que reciba donantes las 24 horas.
-- Coordenadas:
--   Se completaron únicamente cuando fue posible ubicar el establecimiento con
--   suficiente confianza mediante documentación institucional, enlaces cartográficos
--   publicados o directorios que exponen coordenadas/Plus Codes.
--   Los NULL restantes son intencionales: no se sustituyeron por el centro de la ciudad
--   ni por coordenadas aproximadas para evitar falsos resultados en "cerca de mí".
-- Correcciones geográficas:
--   * Hospital Municipal Dr. Jacinto Ignacio Mañón -> DN-01.
--   * Hospital Municipal Dr. Luis Bonilla Castillo (Matancitas) -> MTS-01.

SET TIME ZONE 'America/Santo_Domingo';

INSERT INTO donation_centers (
    name, type, province_id, municipality_id, sector, address,
    reference, phone, schedule, latitude, longitude, active
) VALUES
    ('Centro de la Sangre y Especialidades Sucursal II', 'BLOOD_BANK', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. 27 de Febrero, Santo Domingo', NULL, '809-332-1043', '24 horas', 18.4762737, -69.9123154, TRUE),
    ('Hemonet Banco de Sangre y Unidad de Aféresis', 'BLOOD_BANK', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. Dr. Bernardo Correa y Cidrón 57, Santo Domingo', NULL, '809-466-6313', '24 horas', 18.4561953, -69.925839, TRUE),
    ('Banco de Sangre y Unidad de Hemoterapia Vidayelin', 'BLOOD_BANK', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Calle Paseo del Este 60, Santo Domingo', NULL, '809-692-6317', '24 horas', 18.4925562, -69.9373221, TRUE),
    ('Hospital General de la Plaza de la Salud', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), 'La Fe', 'Av. José Ortega y Gasset, Santo Domingo', NULL, '809-565-7477', '24 horas', 18.4883828, -69.9216244, TRUE),
    ('Hospital Padre Billini', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), 'Ciudad Colonial', 'C. Santomé, Santo Domingo', NULL, '809-333-5656', '24 horas', 18.4714000, -69.8891100, TRUE),
    ('Hospital Dr. Francisco Moscoso Puello', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Santo Domingo, Distrito Nacional', NULL, NULL, '24 horas', 18.5015094, -69.9012369, TRUE),
    ('Hospital Dr. Robert Reid Cabral', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Santo Domingo, Distrito Nacional', NULL, NULL, '24 horas', 18.4529838, -69.9235408, TRUE),
    ('Hospital Docente Universitario Maternidad Nuestra Señora de la Altagracia', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. Pedro Henríquez Ureña 49, Santo Domingo', NULL, '809-686-6376', '24 horas', 18.4730298, -69.9090683, TRUE),
    ('Centro Médico UCE', 'MEDICAL_CENTER', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. Máximo Gómez 66, Santo Domingo', NULL, '809-221-0171', '24 horas', 18.4715200, -69.9128900, TRUE),
    ('Hospiten Santo Domingo', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. Alma Máter, Santo Domingo', NULL, '809-541-3000', '24 horas', 18.4652700, -69.9212800, TRUE),
    ('Hemocentro Nacional', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-02'), NULL, 'Ciudad Sanitaria Dra. Evangelina Rodríguez, Av. Los Restauradores, Santo Domingo Norte', 'Ciudad Sanitaria Dra. Evangelina Rodríguez', '809-793-7503', 'Lun-Vie 07:00-19:00; Sáb 08:00-12:00', NULL, NULL, TRUE),
    ('Banco de Sangre Hemo Family', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-01'), NULL, 'Respaldo Las Américas 60 esq. 5, Santo Domingo Este', NULL, '809-598-5209', '24 horas', 18.4847304, -69.8691574, TRUE),
    ('Centro de la Sangre y Especialidades', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-01'), NULL, 'Av. Sabana Larga 55, Santo Domingo Este', NULL, '809-273-7340', '24 horas', 18.4847555, -69.8768822, TRUE),
    ('Banco de Sangre CrisNey', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-01'), NULL, 'Av. Sabana Larga 31, Santo Domingo Este', NULL, '809-595-9196', '24 horas', 18.4884396, -69.8679454, TRUE),
    ('Referencia Banco de Sangre - Sede Central', 'BLOOD_BANK', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-03'), 'Zona Industrial de Herrera', 'Av. Luperón No. 3 esq. Av. Mirador Sur, Santo Domingo Oeste', NULL, '809-221-5545', '24 horas', 18.4884598, -69.8679454, TRUE),
    ('Hospital Traumatológico Dr. Ney Arias Lora', 'HOSPITAL', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-02'), NULL, 'Santo Domingo Norte, Santo Domingo', NULL, NULL, '24 horas', 18.5473704, -69.8838615, TRUE),
    ('Hospital Pediátrico Dr. Hugo Mendoza', 'HOSPITAL', 2, (SELECT id FROM municipalities WHERE province_id=2 AND code='SD-02'), NULL, 'Santo Domingo Norte, Santo Domingo', NULL, NULL, '24 horas', 18.5465000, -69.8820690, TRUE),
    ('Hospital Municipal Dr. Jacinto Ignacio Mañón', 'HOSPITAL', 1, (SELECT id FROM municipalities WHERE province_id=1 AND code='DN-01'), NULL, 'Av. República de Colombia, Santo Domingo Oeste', NULL, '809-930-0034', '24 horas', 18.5067300, -69.9890800, TRUE),
    ('Hospital Regional Universitario José María Cabral y Báez', 'HOSPITAL', 3, (SELECT id FROM municipalities WHERE province_id=3 AND code='STG-01'), NULL, 'Santiago de los Caballeros, Santiago', NULL, NULL, '24 horas', 19.4560208, -70.6993762, TRUE),
    ('Hospital Infantil Regional Universitario Dr. Arturo Grullón', 'HOSPITAL', 3, (SELECT id FROM municipalities WHERE province_id=3 AND code='STG-01'), NULL, 'Santiago de los Caballeros, Santiago', NULL, NULL, '24 horas', 19.4660845, -70.7073222, TRUE),
    ('Hospital Presidente Estrella Ureña', 'HOSPITAL', 3, (SELECT id FROM municipalities WHERE province_id=3 AND code='STG-01'), NULL, 'Santiago de los Caballeros, Santiago', NULL, NULL, '24 horas', 19.4674200, -70.7112400, TRUE),
    ('Clínica Unión Médica del Norte', 'CLINIC', 3, (SELECT id FROM municipalities WHERE province_id=3 AND code='STG-01'), NULL, 'Santiago de los Caballeros, Santiago', NULL, NULL, '24 horas', 19.4595937, -70.6800461, TRUE),
    ('Hospital Metropolitano de Santiago - HOMS', 'HOSPITAL', 3, (SELECT id FROM municipalities WHERE province_id=3 AND code='STG-01'), NULL, 'Santiago de los Caballeros, Santiago', NULL, NULL, '24 horas', 19.4356953, -70.6613081, TRUE),
    ('Hospital Regional Dr. Luis Manuel Morillo King', 'HOSPITAL', 4, (SELECT id FROM municipalities WHERE province_id=4 AND code='VEG-01'), NULL, 'La Vega, La Vega', NULL, NULL, '24 horas', 19.2192972, -70.5216592, TRUE),
    ('Hospital Traumatológico y Quirúrgico Prof. Juan Bosch', 'HOSPITAL', 4, (SELECT id FROM municipalities WHERE province_id=4 AND code='VEG-01'), NULL, 'La Vega, La Vega', NULL, NULL, '24 horas', 19.1416106, -70.4671615, TRUE),
    ('Hospital Regional Universitario San Vicente de Paúl', 'HOSPITAL', 7, (SELECT id FROM municipalities WHERE province_id=7 AND code='DU-01'), NULL, 'San Francisco de Macorís, Duarte', NULL, NULL, '24 horas', 19.3053849, -70.2564260, TRUE),
    ('Hospital Municipal Dr. Federico Leopold Lavandier', 'HOSPITAL', 7, (SELECT id FROM municipalities WHERE province_id=7 AND code='DU-07'), NULL, 'Villa Riva, Duarte', NULL, NULL, '24 horas', 19.2804675, -70.24447, TRUE),
    ('Hospital Provincial Ricardo Limardo', 'HOSPITAL', 6, (SELECT id FROM municipalities WHERE province_id=6 AND code='PP-01'), NULL, 'Puerto Plata, Puerto Plata', NULL, NULL, '24 horas', 19.7859514, -70.6882503, TRUE),
    ('Hospital Dr. Toribio Bencosme', 'HOSPITAL', 11, (SELECT id FROM municipalities WHERE province_id=11 AND code='ESP-01'), NULL, 'Moca, Espaillat', NULL, NULL, '24 horas', 19.3890694, -70.5186843, TRUE),
    ('Hospital Dr. Antonio Yapor Heded', 'HOSPITAL', 19, (SELECT id FROM municipalities WHERE province_id=19 AND code='MTS-01'), NULL, 'Nagua, María Trinidad Sánchez', NULL, NULL, '24 horas', 19.3654315, -69.8506201, TRUE),
    ('Hospital Municipal Dr. Luis Bonilla Castillo', 'HOSPITAL', 19, (SELECT id FROM municipalities WHERE province_id=19 AND code='MTS-01'), NULL, 'Cabrera, María Trinidad Sánchez', NULL, NULL, '24 horas', 19.3543125, -69.8319531, TRUE),
    ('Hospital Provincial Inmaculada Concepción', 'HOSPITAL', 26, (SELECT id FROM municipalities WHERE province_id=26 AND code='SCR-01'), NULL, 'Cotuí, Sánchez Ramírez', NULL, NULL, '24 horas', 19.0485813, -70.1520301, TRUE),
    ('Hospital Provincial Pedro Emilio de Marchena', 'HOSPITAL', 12, (SELECT id FROM municipalities WHERE province_id=12 AND code='MN-01'), NULL, 'Bonao, Monseñor Nouel', NULL, NULL, '24 horas', 18.9368505, -70.4117632, TRUE),
    ('Hospital Regional Ing. Luis L. Bogaert', 'HOSPITAL', 22, (SELECT id FROM municipalities WHERE province_id=22 AND code='VAL-01'), NULL, 'Mao, Valverde', NULL, NULL, '24 horas', 19.5389224, -71.0818863, TRUE),
    ('Hospital Provincial General Santiago Rodríguez', 'HOSPITAL', 25, (SELECT id FROM municipalities WHERE province_id=25 AND code='SR-01'), NULL, 'San Ignacio de Sabaneta, Santiago Rodríguez', NULL, NULL, '24 horas', 19.4783258, -71.3416475, TRUE),
    ('Hospital Provincial Ramón Matías Mella', 'HOSPITAL', 24, (SELECT id FROM municipalities WHERE province_id=24 AND code='DAJ-01'), NULL, 'Dajabón, Dajabón', NULL, NULL, '24 horas', 19.5489449, -71.7105451, TRUE),
    ('Hospital Provincial Padre Fantino', 'HOSPITAL', 23, (SELECT id FROM municipalities WHERE province_id=23 AND code='MC-01'), NULL, 'San Fernando de Monte Cristi, Monte Cristi', NULL, NULL, '24 horas', 19.8507900, -71.6452200, TRUE),
    ('Hospital Provincial Dr. Pascasio Toribio Piantini', 'HOSPITAL', 27, (SELECT id FROM municipalities WHERE province_id=27 AND code='HMI-01'), NULL, 'Salcedo, Hermanas Mirabal', NULL, NULL, '24 horas', 19.3735305, -70.4195195, TRUE),
    ('Hospital Provincial Dr. Leopoldo Pou', 'HOSPITAL', 18, (SELECT id FROM municipalities WHERE province_id=18 AND code='SAM-01'), NULL, 'Santa Bárbara de Samaná, Samaná', NULL, NULL, '24 horas', 19.2054537, -69.3372941, TRUE),
    ('Hospital Regional Juan Pablo Pina', 'HOSPITAL', 5, (SELECT id FROM municipalities WHERE province_id=5 AND code='SC-01'), NULL, 'San Cristóbal, San Cristóbal', NULL, NULL, '24 horas', 18.4186068, -70.1146949, TRUE),
    ('Hospital Provincial Nuestra Señora de Regla', 'HOSPITAL', 13, (SELECT id FROM municipalities WHERE province_id=13 AND code='PER-01'), NULL, 'Baní, Peravia', NULL, NULL, '24 horas', 18.2792229, -70.3396030, TRUE),
    ('Hospital Provincial San José de Ocoa', 'HOSPITAL', 32, (SELECT id FROM municipalities WHERE province_id=32 AND code='SJO-01'), NULL, 'San José de Ocoa, San José de Ocoa', NULL, NULL, '24 horas', 18.5429813, -70.5084622, TRUE),
    ('Hospital Regional Taiwán 19 de Marzo', 'HOSPITAL', 14, (SELECT id FROM municipalities WHERE province_id=14 AND code='AZ-01'), NULL, 'Azua de Compostela, Azua', NULL, NULL, '24 horas', 18.4509967, -70.7393349, TRUE),
    ('Hospital Municipal Nuestra Señora del Carmen', 'HOSPITAL', 14, (SELECT id FROM municipalities WHERE province_id=14 AND code='AZ-06'), NULL, 'Padre Las Casas, Azua', NULL, NULL, '24 horas', 18.7296847, -70.9441217, TRUE),
    ('Hospital Regional Dr. Alejandro Cabral', 'HOSPITAL', 16, (SELECT id FROM municipalities WHERE province_id=16 AND code='SJ-01'), NULL, 'San Juan de la Maguana, San Juan', NULL, NULL, '24 horas', 18.8021987, -71.2246820, TRUE),
    ('Hospital Provincial Rosa Duarte', 'HOSPITAL', 31, (SELECT id FROM municipalities WHERE province_id=31 AND code='EP-01'), NULL, 'Comendador, Elías Piña', NULL, NULL, '24 horas', 18.8753654, -71.6978846, TRUE),
    ('Hospital Regional Universitario Jaime Mota', 'HOSPITAL', 15, (SELECT id FROM municipalities WHERE province_id=15 AND code='BAR-01'), NULL, 'Santa Cruz de Barahona, Barahona', NULL, NULL, '24 horas', 18.2098647, -71.1043937, TRUE),
    ('Hospital Provincial Dr. Elio Fiallo', 'HOSPITAL', 30, (SELECT id FROM municipalities WHERE province_id=30 AND code='PED-01'), NULL, 'Pedernales, Pedernales', NULL, NULL, '24 horas', 18.0330967, -71.7458403, TRUE),
    ('Hospital Provincial General Melenciano', 'HOSPITAL', 29, (SELECT id FROM municipalities WHERE province_id=29 AND code='IND-01'), NULL, 'Jimaní, Independencia', NULL, NULL, '24 horas', 18.4929634, -71.8513041, TRUE),
    ('Hospital Provincial San Bartolomé', 'HOSPITAL', 28, (SELECT id FROM municipalities WHERE province_id=28 AND code='BAH-01'), NULL, 'Neiba, Bahoruco', NULL, NULL, '24 horas', 18.4824266, -71.4141659, TRUE),
    ('Hospital Regional Dr. Antonio Musa', 'HOSPITAL', 10, (SELECT id FROM municipalities WHERE province_id=10 AND code='SPM-01'), NULL, 'San Pedro de Macorís, San Pedro de Macorís', NULL, NULL, '24 horas', 18.4691599, -69.3082404, TRUE),
    ('Hospital Provincial Dr. Arístides Fiallo Cabral', 'HOSPITAL', 8, (SELECT id FROM municipalities WHERE province_id=8 AND code='LR-01'), NULL, 'La Romana, La Romana', NULL, NULL, '24 horas', 18.4292609, -68.9671848, TRUE),
    ('Centro Médico Central Romana', 'MEDICAL_CENTER', 8, (SELECT id FROM municipalities WHERE province_id=8 AND code='LR-01'), NULL, 'La Romana, La Romana', NULL, NULL, '24 horas', 18.4190681, -68.966128, TRUE),
    ('Hospital General y de Especialidades Nuestra Señora de la Altagracia', 'HOSPITAL', 9, (SELECT id FROM municipalities WHERE province_id=9 AND code='LA-01'), NULL, 'Higüey, La Altagracia', NULL, NULL, '24 horas', 18.6025039, -68.7151580, TRUE),
    ('Referencia Banco de Sangre - Centro Punta Cana', 'BLOOD_BANK', 9, (SELECT id FROM municipalities WHERE province_id=9 AND code='LA-01'), 'Punta Cana', 'Av. Boulevard 1ro. de Noviembre, Punta Cana Village, Edif. Belanova', 'Punta Cana Village', '809-959-2095', '24 horas, 365 días', 18.5557727, -68.3722915, TRUE),
    ('Hospital Provincial Dr. Teófilo Hernández', 'HOSPITAL', 21, (SELECT id FROM municipalities WHERE province_id=21 AND code='ES-01'), NULL, 'Santa Cruz de El Seibo, El Seibo', NULL, NULL, '24 horas', 18.7581604, -69.0347601, TRUE),
    ('Hospital Provincial Dr. Leopoldo Martínez', 'HOSPITAL', 20, (SELECT id FROM municipalities WHERE province_id=20 AND code='HMA-01'), NULL, 'Hato Mayor del Rey, Hato Mayor', NULL, NULL, '24 horas', 18.7608973, -69.2526643, TRUE),
    ('Hospital Provincial Dr. Ángel Contreras', 'HOSPITAL', 17, (SELECT id FROM municipalities WHERE province_id=17 AND code='MP-01'), NULL, 'Monte Plata, Monte Plata', NULL, NULL, '24 horas', 18.8031100, -69.7860600, TRUE);
