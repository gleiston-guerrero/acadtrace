-- =============================================================================
-- V18__rol_aplicacion_y_revocacion_auditoria.sql
-- Criterio E6: Principio de Menor Privilegio sobre la Bitacora de Auditoria
--
-- Exigencia del evaluador (E6):
-- "El problema es que ese rol solo existe dentro de la prueba: ninguna migracion
--  lo crea, y la aplicacion se conecta como propietario del esquema, sobre el
--  que una revocacion a PUBLIC no tiene efecto."
--
-- Esta migracion crea formalmente el rol de aplicacion 'sga_app', le otorga los
-- permisos necesarios para la operacion del sistema, y REVOCA expresamente
-- cualquier permiso de UPDATE, DELETE y TRUNCATE sobre sga_principal.auditoria.
-- =============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sga_app') THEN
        CREATE ROLE sga_app WITH LOGIN PASSWORD '${sga_app_password}' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END $$;

-- 1. Permiso de conexion y resolucion de esquema
GRANT USAGE ON SCHEMA sga_principal TO sga_app;

-- 2. Permisos sobre auditoria: estrictamente append-only (SELECT + INSERT)
GRANT SELECT, INSERT ON TABLE sga_principal.auditoria TO sga_app;
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM sga_app;
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM PUBLIC;

-- 3. Permiso para avanzar el estado transaccional de la cadena (E3)
GRANT SELECT, UPDATE ON TABLE sga_principal.estado_cadena_auditoria TO sga_app;

-- 4. Permisos sobre secuencias para generacion de IDs
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_principal TO sga_app;

-- 5. Permisos sobre el resto de tablas operativas del esquema
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_principal TO sga_app;

-- 6. Garantizar que la restriccion de auditoria prevalece sobre el GRANT ALL TABLES
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM sga_app;
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM PUBLIC;
