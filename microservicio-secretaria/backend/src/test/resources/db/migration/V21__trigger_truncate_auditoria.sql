-- =============================================================================
-- V21__trigger_truncate_auditoria.sql
--
-- Complementa el disparador de inmutabilidad de la bitacora.
--
-- V13 creo tg_auditoria_append_only como BEFORE UPDATE OR DELETE ... FOR EACH
-- ROW. Los disparadores de fila NO se ejecutan ante un TRUNCATE, asi que el
-- propietario de la tabla puede vaciar sga_principal.auditoria completa sin
-- que salte el rechazo. El ingeniero lo reprodujo en la evaluacion.
--
-- V21 anade un disparador de sentencia (FOR EACH STATEMENT) sobre BEFORE
-- TRUNCATE ON sga_principal.auditoria, que rechaza la operacion con el
-- mismo mensaje "Operacion rechazada" que usa el disparador de V13.
-- =============================================================================

CREATE OR REPLACE FUNCTION sga_principal.prohibir_truncate_auditoria()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Operacion rechazada: TRUNCATE sobre sga_principal.auditoria no esta permitida';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_auditoria_no_truncate ON sga_principal.auditoria;

CREATE TRIGGER tg_auditoria_no_truncate
    BEFORE TRUNCATE ON sga_principal.auditoria
    FOR EACH STATEMENT
    EXECUTE FUNCTION sga_principal.prohibir_truncate_auditoria();
