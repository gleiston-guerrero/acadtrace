-- Campos faltantes en la tabla estudiantes
ALTER TABLE sga_principal.estudiantes
    ADD COLUMN IF NOT EXISTS carnet_conadis VARCHAR(30),
    ADD COLUMN IF NOT EXISTS nacionalidad VARCHAR(50),
    ADD COLUMN IF NOT EXISTS etnia VARCHAR(50),
    ADD COLUMN IF NOT EXISTS lugar_nacimiento VARCHAR(150),
    ADD COLUMN IF NOT EXISTS vive_con VARCHAR(50),
    ADD COLUMN IF NOT EXISTS numeros_hermanos SMALLINT,
    ADD COLUMN IF NOT EXISTS beneficio_social BOOLEAN DEFAULT FALSE;
