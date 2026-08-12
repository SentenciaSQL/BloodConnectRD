-- Configura la zona horaria usada por esta migración.
SET TIME ZONE 'America/Santo_Domingo';

-- Almacena las provincias de la República Dominicana.
CREATE TABLE provinces (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Almacena los municipios asociados a cada provincia.
CREATE TABLE municipalities (
    id BIGSERIAL PRIMARY KEY,
    province_id BIGINT NOT NULL REFERENCES provinces(id),
    code VARCHAR(20) NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (province_id, name),
    UNIQUE (province_id, code)
);

-- Registra las 32 provincias con identificadores estables para sus municipios.
INSERT INTO provinces (id, code, name) VALUES
    (1, 'DN', 'Distrito Nacional'),
    (2, 'SD', 'Santo Domingo'),
    (3, 'STG', 'Santiago'),
    (4, 'VEG', 'La Vega'),
    (5, 'SC', 'San Cristóbal'),
    (6, 'PP', 'Puerto Plata'),
    (7, 'DU', 'Duarte'),
    (8, 'LR', 'La Romana'),
    (9, 'LA', 'La Altagracia'),
    (10, 'SPM', 'San Pedro de Macorís'),
    (11, 'ESP', 'Espaillat'),
    (12, 'MN', 'Monseñor Nouel'),
    (13, 'PER', 'Peravia'),
    (14, 'AZ', 'Azua'),
    (15, 'BAR', 'Barahona'),
    (16, 'SJ', 'San Juan'),
    (17, 'MP', 'Monte Plata'),
    (18, 'SAM', 'Samaná'),
    (19, 'MTS', 'María Trinidad Sánchez'),
    (20, 'HMA', 'Hato Mayor'),
    (21, 'ES', 'El Seibo'),
    (22, 'VAL', 'Valverde'),
    (23, 'MC', 'Monte Cristi'),
    (24, 'DAJ', 'Dajabón'),
    (25, 'SR', 'Santiago Rodríguez'),
    (26, 'SCR', 'Sánchez Ramírez'),
    (27, 'HMI', 'Hermanas Mirabal'),
    (28, 'BAH', 'Bahoruco'),
    (29, 'IND', 'Independencia'),
    (30, 'PED', 'Pedernales'),
    (31, 'EP', 'Elías Piña'),
    (32, 'SJO', 'San José de Ocoa');

-- Sincroniza la secuencia después de insertar identificadores explícitos.
SELECT setval(
    pg_get_serial_sequence('provinces', 'id'),
    (SELECT MAX(id) FROM provinces)
);

-- Registra los principales municipios de cada provincia.
INSERT INTO municipalities (province_id, code, name) VALUES
    (1, 'DN-01', 'Distrito Nacional (Santo Domingo de Guzmán)'),

    (2, 'SD-01', 'Santo Domingo Este'),
    (2, 'SD-02', 'Santo Domingo Norte'),
    (2, 'SD-03', 'Santo Domingo Oeste'),
    (2, 'SD-04', 'Boca Chica'),
    (2, 'SD-05', 'Los Alcarrizos'),
    (2, 'SD-06', 'Pedro Brand'),
    (2, 'SD-07', 'San Antonio de Guerra'),
    (2, 'SD-08', 'Guerra'),

    (3, 'STG-01', 'Santiago'),
    (3, 'STG-02', 'Villa Bisonó'),
    (3, 'STG-03', 'Tamboril'),
    (3, 'STG-04', 'Puñal'),
    (3, 'STG-05', 'Licey al Medio'),
    (3, 'STG-06', 'Villa González'),
    (3, 'STG-07', 'San José de Las Matas'),
    (3, 'STG-08', 'Jánico'),
    (3, 'STG-09', 'Sabana Iglesia'),
    (3, 'STG-10', 'Baitoa'),

    (4, 'VEG-01', 'La Vega'),
    (4, 'VEG-02', 'Constanza'),
    (4, 'VEG-03', 'Jarabacoa'),
    (4, 'VEG-04', 'Jima Abajo'),

    (5, 'SC-01', 'San Cristóbal'),
    (5, 'SC-02', 'Bajos de Haina'),
    (5, 'SC-03', 'Cambita Garabitos'),
    (5, 'SC-04', 'Los Cacaos'),
    (5, 'SC-05', 'Sabana Grande de Palenque'),
    (5, 'SC-06', 'San Gregorio de Nigua'),
    (5, 'SC-07', 'Villa Altagracia'),
    (5, 'SC-08', 'Yaguate'),

    (6, 'PP-01', 'Puerto Plata'),
    (6, 'PP-02', 'Altamira'),
    (6, 'PP-03', 'Guananico'),
    (6, 'PP-04', 'Imbert'),
    (6, 'PP-05', 'Los Hidalgos'),
    (6, 'PP-06', 'Luperón'),
    (6, 'PP-07', 'Sosúa'),
    (6, 'PP-08', 'Villa Isabela'),
    (6, 'PP-09', 'Villa Montellano'),

    (7, 'DU-01', 'San Francisco de Macorís'),
    (7, 'DU-02', 'Arenoso'),
    (7, 'DU-03', 'Castillo'),
    (7, 'DU-04', 'Eugenio María de Hostos'),
    (7, 'DU-05', 'Las Guáranas'),
    (7, 'DU-06', 'Pimentel'),
    (7, 'DU-07', 'Villa Riva'),

    (8, 'LR-01', 'La Romana'),
    (8, 'LR-02', 'Guaymate'),
    (8, 'LR-03', 'Villa Hermosa'),

    (9, 'LA-01', 'Higüey'),
    (9, 'LA-02', 'San Rafael del Yuma'),

    (10, 'SPM-01', 'San Pedro de Macorís'),
    (10, 'SPM-02', 'Consuelo'),
    (10, 'SPM-03', 'Guayacanes'),
    (10, 'SPM-04', 'Quisqueya'),
    (10, 'SPM-05', 'Ramón Santana'),
    (10, 'SPM-06', 'San José de los Llanos'),

    (11, 'ESP-01', 'Moca'),
    (11, 'ESP-02', 'Cayetano Germosén'),
    (11, 'ESP-03', 'Gaspar Hernández'),
    (11, 'ESP-04', 'Jamao al Norte'),
    (11, 'ESP-05', 'San Víctor'),

    (12, 'MN-01', 'Bonao'),
    (12, 'MN-02', 'Maimón'),
    (12, 'MN-03', 'Piedra Blanca'),

    (13, 'PER-01', 'Baní'),
    (13, 'PER-02', 'Nizao'),
    (13, 'PER-03', 'Matanzas'),

    (14, 'AZ-01', 'Azua de Compostela'),
    (14, 'AZ-02', 'Estebanía'),
    (14, 'AZ-03', 'Guayabal'),
    (14, 'AZ-04', 'Las Charcas'),
    (14, 'AZ-05', 'Las Yayas de Viajama'),
    (14, 'AZ-06', 'Padre Las Casas'),
    (14, 'AZ-07', 'Peralta'),
    (14, 'AZ-08', 'Pueblo Viejo'),
    (14, 'AZ-09', 'Sabana Yegua'),
    (14, 'AZ-10', 'Tábara Arriba'),

    (15, 'BAR-01', 'Barahona'),
    (15, 'BAR-02', 'Cabral'),
    (15, 'BAR-03', 'El Peñón'),
    (15, 'BAR-04', 'Enriquillo'),
    (15, 'BAR-05', 'Fundación'),
    (15, 'BAR-06', 'Jaquimeyes'),
    (15, 'BAR-07', 'La Ciénaga'),
    (15, 'BAR-08', 'Las Salinas'),
    (15, 'BAR-09', 'Paraíso'),
    (15, 'BAR-10', 'Polo'),
    (15, 'BAR-11', 'Vicente Noble'),

    (16, 'SJ-01', 'San Juan de la Maguana'),
    (16, 'SJ-02', 'Bohechío'),
    (16, 'SJ-03', 'El Cercado'),
    (16, 'SJ-04', 'Juan de Herrera'),
    (16, 'SJ-05', 'Las Matas de Farfán'),
    (16, 'SJ-06', 'Vallejuelo'),

    (17, 'MP-01', 'Monte Plata'),
    (17, 'MP-02', 'Bayaguana'),
    (17, 'MP-03', 'Peralvillo'),
    (17, 'MP-04', 'Sabana Grande de Boyá'),
    (17, 'MP-05', 'Yamasá'),

    (18, 'SAM-01', 'Samaná'),
    (18, 'SAM-02', 'Las Terrenas'),
    (18, 'SAM-03', 'Sánchez'),

    (19, 'MTS-01', 'Nagua'),
    (19, 'MTS-02', 'Cabrera'),
    (19, 'MTS-03', 'El Factor'),
    (19, 'MTS-04', 'Río San Juan'),

    (20, 'HMA-01', 'Hato Mayor del Rey'),
    (20, 'HMA-02', 'El Valle'),
    (20, 'HMA-03', 'Sabana de la Mar'),

    (21, 'ES-01', 'El Seibo'),
    (21, 'ES-02', 'Miches'),

    (22, 'VAL-01', 'Mao'),
    (22, 'VAL-02', 'Esperanza'),
    (22, 'VAL-03', 'Laguna Salada'),

    (23, 'MC-01', 'Monte Cristi'),
    (23, 'MC-02', 'Castañuelas'),
    (23, 'MC-03', 'Guayubín'),
    (23, 'MC-04', 'Las Matas de Santa Cruz'),
    (23, 'MC-05', 'Pepillo Salcedo'),
    (23, 'MC-06', 'Villa Vásquez'),

    (24, 'DAJ-01', 'Dajabón'),
    (24, 'DAJ-02', 'El Pino'),
    (24, 'DAJ-03', 'Loma de Cabrera'),
    (24, 'DAJ-04', 'Partido'),
    (24, 'DAJ-05', 'Restauración'),

    (25, 'SR-01', 'Sabaneta'),
    (25, 'SR-02', 'Monción'),
    (25, 'SR-03', 'Villa Los Almácigos'),

    (26, 'SCR-01', 'Cotuí'),
    (26, 'SCR-02', 'Cevicos'),
    (26, 'SCR-03', 'Fantino'),
    (26, 'SCR-04', 'La Mata'),

    (27, 'HMI-01', 'Salcedo'),
    (27, 'HMI-02', 'Tenares'),
    (27, 'HMI-03', 'Villa Tapia'),

    (28, 'BAH-01', 'Neiba'),
    (28, 'BAH-02', 'Galván'),
    (28, 'BAH-03', 'Los Ríos'),
    (28, 'BAH-04', 'Tamayo'),
    (28, 'BAH-05', 'Villa Jaragua'),

    (29, 'IND-01', 'Jimaní'),
    (29, 'IND-02', 'Cristóbal'),
    (29, 'IND-03', 'Duvergé'),
    (29, 'IND-04', 'La Descubierta'),
    (29, 'IND-05', 'Mella'),
    (29, 'IND-06', 'Postrer Río'),

    (30, 'PED-01', 'Pedernales'),
    (30, 'PED-02', 'Oviedo'),

    (31, 'EP-01', 'Comendador'),
    (31, 'EP-02', 'Bánica'),
    (31, 'EP-03', 'El Llano'),
    (31, 'EP-04', 'Hondo Valle'),
    (31, 'EP-05', 'Juan Santiago'),
    (31, 'EP-06', 'Pedro Santana'),

    (32, 'SJO-01', 'San José de Ocoa'),
    (32, 'SJO-02', 'Rancho Arriba'),
    (32, 'SJO-03', 'Sabana Larga');
