INSERT INTO sga_principal.roles (nombre, descripcion, activo)
SELECT 'ROLE_REPRESENTANTE', 'Representante legal con acceso de consulta', true
WHERE NOT EXISTS (
    SELECT 1 FROM sga_principal.roles WHERE nombre = 'ROLE_REPRESENTANTE'
);

ALTER TABLE sga_principal.representantes
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_representantes_id_usuario
    ON sga_principal.representantes (id_usuario)
    WHERE id_usuario IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_representantes_usuario'
    ) THEN
        ALTER TABLE sga_principal.representantes
            ADD CONSTRAINT fk_representantes_usuario
            FOREIGN KEY (id_usuario)
            REFERENCES sga_principal.usuarios(id_usuario);
    END IF;
END $$;
