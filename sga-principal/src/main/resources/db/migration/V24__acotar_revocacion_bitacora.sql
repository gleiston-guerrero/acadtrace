-- V24__acotar_revocacion_bitacora.sql
-- Se revierte la revocacion global sobre todas las tablas futuras del esquema sga_principal
-- que fue introducida en V19, ya que rompia los privilegios de operacion normal de sga_app.
-- En su lugar, se reafirma la restriccion exclusivamente sobre la tabla de bitacora.

ALTER DEFAULT PRIVILEGES IN SCHEMA sga_principal GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO sga_app;
REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM sga_app;
