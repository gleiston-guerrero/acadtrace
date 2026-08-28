-- ============================================================
-- V8__auditoria_distribuido.sql
--
-- Extiende sga_principal.auditoria (creada en FIX-3 para PRINCIPAL y
-- DOCENTE, pero nunca conectada a codigo: existe la tabla y el
-- repositorio JPA, pero nada la escribia) para:
--   1. Aceptar microservicio-secretaria como origen valido.
--   2. Correlacionar un mismo evento de negocio a traves de varios
--      microservicios via trace_id (ej. secretaria dispara un update
--      que llega por gRPC a sga-principal: ambas filas comparten
--      trace_id).
--   3. Distinguir intentos fallidos (login incorrecto, token interno
--      invalido entre microservicios) de eventos exitosos.
--   4. Nuevas acciones para cubrir login fallido, asignacion de roles
--      y llamadas gRPC entre microservicios.
--
-- La tabla esta vacia en produccion (0 filas), asi que estos cambios
-- son seguros y no requieren backfill.
-- ============================================================

-- Los nuevos valores de enum deben ir fuera de un bloque de transaccion
-- explicito (Postgres no permite usarlos en la misma transaccion en la
-- que se agregan si van junto a otras sentencias).
ALTER TYPE sga_principal.accion_auditoria_t ADD VALUE IF NOT EXISTS 'LOGIN_FALLIDO';
ALTER TYPE sga_principal.accion_auditoria_t ADD VALUE IF NOT EXISTS 'ROL_ASIGNADO';
ALTER TYPE sga_principal.accion_auditoria_t ADD VALUE IF NOT EXISTS 'LLAMADA_GRPC';

BEGIN;

ALTER TABLE sga_principal.auditoria
    DROP CONSTRAINT auditoria_schema_origen_check,
    ADD CONSTRAINT auditoria_schema_origen_check
        CHECK (schema_origen IN ('PRINCIPAL', 'DOCENTE', 'SECRETARIA'));

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS trace_id  UUID        NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS resultado VARCHAR(10) NOT NULL DEFAULT 'EXITO';

ALTER TABLE sga_principal.auditoria
    ADD CONSTRAINT ck_auditoria_resultado CHECK (resultado IN ('EXITO', 'FALLO'));

CREATE INDEX IF NOT EXISTS ix_auditoria_trace ON sga_principal.auditoria (trace_id);

COMMENT ON COLUMN sga_principal.auditoria.trace_id IS 'Correlaciona eventos de un mismo request a traves de microservicios (header X-Trace-Id / metadata gRPC).';
COMMENT ON COLUMN sga_principal.auditoria.resultado IS 'EXITO o FALLO del evento auditado (ej. login fallido, token interno invalido).';
COMMENT ON COLUMN sga_principal.auditoria.hmac IS 'HMAC-SHA256 (hex) sobre los campos clave de la fila, firmado con jwt.secret. Verifica que el registro no fue alterado manualmente.';

COMMIT;
