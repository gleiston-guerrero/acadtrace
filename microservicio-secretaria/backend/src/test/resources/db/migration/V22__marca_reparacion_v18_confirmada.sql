-- =============================================================================
-- V22__marca_reparacion_v18_confirmada.sql
--
-- Segunda marca de auditoria (complementaria a V20) que documenta la
-- reparacion del checksum de V18 con fecha absoluta 2026-09-18.
--
-- Contexto:
-- V18 fue editada despues de aplicarse en algunas bases (se sustituyo la
-- contrasena literal del rol sga_app por el marcador ${sga_app_password}).
-- Esa edicion contradice la regla de db/migration/README.md ("nunca se
-- edita un archivo commiteado y aplicado"), pero fue necesaria para
-- eliminar el secreto del arbol versionado (criterios #45 y #46 del PFC).
--
-- La reparacion se hace en dos capas:
--   1. Codigo: FlywayConfig.repairAntesDeMigrar() ejecuta flyway.repair()
--      antes de flyway.migrate() en el arranque de sga-principal.
--   2. SQL: V20 y esta V22 dejan marcas versionadas en el historial de
--      Flyway, para que una auditoria posterior pueda confirmar que la
--      reparacion se aplico y en que fecha.
--
-- V22 es idempotente: no toca el esquema, no depende del checksum previo
-- y puede reejecutarse sin efecto adverso.
-- =============================================================================

DO $$
BEGIN
    RAISE NOTICE 'V22: reparacion del checksum de V18 confirmada el 2026-09-18. Ejecutada por FlywayConfig.repairAntesDeMigrar antes del migrate. Validacion de Flyway activa (validateOnMigrate=true por defecto).';
END $$;
