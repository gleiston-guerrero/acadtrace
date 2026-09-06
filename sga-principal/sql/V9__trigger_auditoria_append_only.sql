-- =============================================================================
-- Migracion 007: Inmutabilidad Criptografica Estricta (Append-Only Trigger)
-- Microservicio Secretaria / SGA Principal
--
-- Exigencia Guia de Consolidacion BCEL (Seccion 3):
-- "No hay ningun disparador ni revocacion de permisos que impida actualizar o
--  borrar filas de la tabla de auditoria.
--  Que hacer: anadir disparadores que rechacen actualizacion y borrado sobre esa tabla"
-- =============================================================================

CREATE OR REPLACE FUNCTION sga_principal.prohibir_modificacion_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operacion rechazada: La tabla sga_principal.auditoria es una bitacora inmutable de solo adicion (append-only) protegida bajo estandares ISO/IEC 25010';
END;
$$ LANGUAGE plpgsql;

-- Eliminar si existia previamente para garantizar idempotencia
DROP TRIGGER IF EXISTS tg_auditoria_append_only ON sga_principal.auditoria;

-- Asociar el disparador a eventos de UPDATE y DELETE por cada fila
CREATE TRIGGER tg_auditoria_append_only
BEFORE UPDATE OR DELETE ON sga_principal.auditoria
FOR EACH ROW
EXECUTE FUNCTION sga_principal.prohibir_modificacion_auditoria();

COMMENT ON TRIGGER tg_auditoria_append_only ON sga_principal.auditoria IS 
'Garantiza la inmutabilidad y no-repudio bloqueando cualquier intento de UPDATE o DELETE sobre los registros de auditoria.';
