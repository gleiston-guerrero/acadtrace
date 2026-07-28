-- Bitacora/auditoria: registra cada cambio de estado o asignacion de un ticket.
CREATE TABLE IF NOT EXISTS sga_soporte.historial_ticket (
    id_historial       BIGSERIAL PRIMARY KEY,
    id_ticket          BIGINT      NOT NULL REFERENCES sga_soporte.tickets (id_ticket) ON DELETE CASCADE,
    campo              VARCHAR(30) NOT NULL,   -- ESTADO, ASIGNADO_A, PRIORIDAD...
    valor_anterior     VARCHAR(100),
    valor_nuevo        VARCHAR(100),
    modificado_por     VARCHAR(50) NOT NULL,
    fecha_modificacion TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_historial_ticket ON sga_soporte.historial_ticket (id_ticket);
