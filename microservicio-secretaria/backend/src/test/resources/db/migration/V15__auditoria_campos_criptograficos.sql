-- =============================================================================
-- V15__auditoria_campos_criptograficos.sql
-- Extension del esquema de auditoria centralizado para soportar encadenamiento
-- criptografico SHA-256 (blockchain-style), reloj de Lamport y vector de versiones.
-- Estandarizado segun ADR-007 (ISO/IEC 25010 - Integridad y Trazabilidad).
-- =============================================================================

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS hash_anterior VARCHAR(64),
    ADD COLUMN IF NOT EXISTS hash_actual   VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reloj_lamport BIGINT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS vector_reloj  TEXT;

CREATE INDEX IF NOT EXISTS ix_auditoria_hash_actual ON sga_principal.auditoria (hash_actual);
CREATE INDEX IF NOT EXISTS ix_auditoria_reloj_lamport ON sga_principal.auditoria (reloj_lamport);

COMMENT ON COLUMN sga_principal.auditoria.hash_anterior IS 'Hash SHA-256 del registro de auditoria anterior (64 ceros para bloque genesis)';
COMMENT ON COLUMN sga_principal.auditoria.hash_actual IS 'Hash SHA-256 del registro de auditoria actual';
COMMENT ON COLUMN sga_principal.auditoria.reloj_lamport IS 'Reloj logico escalar de Lamport para ordenamiento temporal total';
COMMENT ON COLUMN sga_principal.auditoria.vector_reloj IS 'Vector de versiones [N_principal, N_docente, N_secretaria] para reconciliacion distribuida';
