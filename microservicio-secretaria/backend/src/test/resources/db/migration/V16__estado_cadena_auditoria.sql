-- =============================================================================
-- V16__estado_cadena_auditoria.sql
-- E3: cabeza transaccional de la cadena de auditoria central.
--
-- La fila singleton permite bloquear la cabeza mediante SELECT ... FOR UPDATE
-- antes de calcular el siguiente eslabon, evitando bifurcaciones concurrentes.
-- =============================================================================

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS contenido_canonico TEXT;

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS version_canonica VARCHAR(20);

CREATE TABLE IF NOT EXISTS sga_principal.estado_cadena_auditoria (
    id_estado SMALLINT PRIMARY KEY,
    ultimo_hash VARCHAR(64) NOT NULL,
    ultimo_lamport BIGINT NOT NULL DEFAULT 0,
    vector_reloj TEXT,
    CONSTRAINT ck_estado_cadena_auditoria_singleton
        CHECK (id_estado = 1)
);

-- Inicializar desde la bitacora existente para no reiniciar la cadena
-- ni el reloj logico cuando esta migracion se aplica sobre una BD con datos.
INSERT INTO sga_principal.estado_cadena_auditoria (
    id_estado,
    ultimo_hash,
    ultimo_lamport,
    vector_reloj
)
SELECT
    1,
    COALESCE(
        (
            SELECT a.hash_actual
            FROM sga_principal.auditoria a
            WHERE a.hash_actual IS NOT NULL
              AND a.hash_actual <> ''
            ORDER BY a.id_auditoria DESC
            LIMIT 1
        ),
        repeat('0', 64)
    ),
    COALESCE(
        (
            SELECT MAX(a.reloj_lamport)
            FROM sga_principal.auditoria a
        ),
        0
    ),
    COALESCE(
        (
            SELECT a.vector_reloj
            FROM sga_principal.auditoria a
            WHERE a.vector_reloj IS NOT NULL
              AND a.vector_reloj <> ''
            ORDER BY a.id_auditoria DESC
            LIMIT 1
        ),
        '{}'
    )
ON CONFLICT (id_estado) DO NOTHING;

COMMENT ON TABLE sga_principal.estado_cadena_auditoria IS
'Cabeza singleton de la cadena de auditoria. Se bloquea con SELECT FOR UPDATE para serializar escritores concurrentes.';

COMMENT ON COLUMN sga_principal.estado_cadena_auditoria.ultimo_hash IS
'Ultimo hash confirmado de la cadena global de auditoria.';

COMMENT ON COLUMN sga_principal.estado_cadena_auditoria.ultimo_lamport IS
'Mayor reloj Lamport confirmado en la cadena global.';


-- E3: los microservicios ejecutan con el usuario de aplicacion.
-- SELECT + UPDATE son necesarios para SELECT ... FOR UPDATE y para
-- avanzar atomicamente la cabeza global.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'sga_user'
    ) THEN
        GRANT SELECT, UPDATE
        ON TABLE sga_principal.estado_cadena_auditoria
        TO sga_user;
    END IF;
END
$$;
