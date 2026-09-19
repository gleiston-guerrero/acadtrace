-- =============================================================================
-- V23__grants_sga_secretaria_y_auditoria_diferidos.sql
--
-- Estas lineas se movieron desde V19 tras la edicion retroactiva del 18/09.
-- =============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA sga_secretaria TO sga_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA sga_secretaria TO sga_app;
GRANT INSERT ON sga_principal.auditoria TO sga_app;
