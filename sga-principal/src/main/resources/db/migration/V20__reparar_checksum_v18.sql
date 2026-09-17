-- =============================================================================
-- V20__reparar_checksum_v18.sql
--
-- Migracion inerte (sin efecto sobre el esquema).
--
-- Contexto:
-- V18 fue editada despues de haberse aplicado en algunas bases (se cambio la
-- contrasena literal por el marcador ${sga_app_password}). Ese cambio altero
-- el checksum del archivo y hace fallar la validacion de Flyway con
-- "Migration checksum mismatch for migration version 18" en cualquier base
-- ya migrada con la version del 13/09.
--
-- No es posible reparar el checksum desde una migracion versionada, porque
-- Flyway valida el historial ANTES de aplicar cualquier migracion nueva,
-- de modo que aborta antes de llegar hasta aqui.
--
-- La reparacion real se hace en el arranque de sga-principal, en
--   ec.edu.uteq.sga.infrastructure.config.FlywayConfig
-- mediante un FlywayMigrationStrategy que ejecuta flyway.repair() antes de
-- flyway.migrate(). Ese componente actualiza el checksum de V18 en el
-- historial y despues aplica normalmente V19, V20 y siguientes.
--
-- V20 se conserva solo para dejar en el historial una marca versionada de
-- la reparacion realizada, con fecha y version identificables por auditoria.
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE 'V20: marca de reparacion aplicada. El checksum de V18 fue reparado por FlywayMigrationStrategy en el arranque de sga-principal.';
END $$;
