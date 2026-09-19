-- =============================================================================
-- V19__ampliar_permisos_sga_app.sql
--
-- Extiende los permisos del rol sga_app a los esquemas sga_docente,
-- sga_secretaria y sga_soporte, para que los cuatro microservicios puedan
-- conectarse con el rol restringido sin necesidad de superusuario.
--
-- Adicionalmente fija ALTER DEFAULT PRIVILEGES en los cuatro esquemas para
-- que las tablas y secuencias creadas por futuras migraciones queden
-- automaticamente accesibles a sga_app.
--
-- La restriccion append-only sobre sga_principal.auditoria se reafirma al
-- final para que ninguna concesion general la anule.
-- =============================================================================

-- Los esquemas de los otros microservicios pueden no existir en instancias
-- de prueba reducidas. Crearlos IF NOT EXISTS mantiene la migracion segura.
CREATE SCHEMA IF NOT EXISTS sga_docente;
CREATE SCHEMA IF NOT EXISTS sga_secretaria;
CREATE SCHEMA IF NOT EXISTS sga_soporte;

-- 1. Acceso a los esquemas de los otros microservicios
GRANT USAGE ON SCHEMA sga_docente     TO sga_app;
GRANT USAGE ON SCHEMA sga_secretaria  TO sga_app;
GRANT USAGE ON SCHEMA sga_soporte     TO sga_app;

-- 2. Permisos operativos sobre las tablas existentes en cada esquema
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_docente     TO sga_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_secretaria  TO sga_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_soporte     TO sga_app;

-- 3. Permisos sobre secuencias existentes en cada esquema
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_docente     TO sga_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_secretaria  TO sga_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_soporte     TO sga_app;

-- 4. Privilegios por omision para tablas y secuencias que se creen mas adelante
ALTER DEFAULT PRIVILEGES IN SCHEMA sga_principal
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA sga_principal
    GRANT USAGE, SELECT ON SEQUENCES TO sga_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA sga_docente
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA sga_docente
    GRANT USAGE, SELECT ON SEQUENCES TO sga_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA sga_secretaria
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA sga_secretaria
    GRANT USAGE, SELECT ON SEQUENCES TO sga_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA sga_soporte
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA sga_soporte
    GRANT USAGE, SELECT ON SEQUENCES TO sga_app;

-- 5. Reafirmar la restriccion append-only sobre la bitacora despues de las
--    concesiones generales, para que sga_app siga sin poder modificarla ni
--    borrarla ni truncarla, ni ahora ni sobre tablas de bitacora futuras.
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM sga_app;
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM PUBLIC;

ALTER DEFAULT PRIVILEGES IN SCHEMA sga_principal
    REVOKE UPDATE, DELETE, TRUNCATE ON TABLES FROM sga_app;

    -- NOTA (2026-09-19): la revocacion por omision amplia sobre todo
    -- sga_principal introducida en las lineas 64-65 fue acotada en
    -- V24__acotar_revocacion_bitacora.sql, que restablece los privilegios
    -- por omision del esquema y deja el REVOKE unicamente sobre auditoria.
