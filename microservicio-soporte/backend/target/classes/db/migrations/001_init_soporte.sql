-- Esquema propio del microservicio de Soporte Tecnico.
-- Toda la data de tickets/incidencias vive aqui; ningun otro servicio la lee
-- directamente (se comparte por gRPC cuando haga falta).
--
-- NOTA: refleja la estructura real ya existente en Supabase. Con IF NOT EXISTS
-- es idempotente: no altera las tablas si ya estan creadas.

CREATE SCHEMA IF NOT EXISTS sga_soporte;

-- Tickets / incidencias de soporte tecnico.
CREATE TABLE IF NOT EXISTS sga_soporte.tickets (
    id_ticket         BIGSERIAL PRIMARY KEY,
    numero_ticket     VARCHAR(30)  UNIQUE,
    titulo            VARCHAR(150) NOT NULL,
    descripcion       TEXT         NOT NULL,
    categoria         VARCHAR(60)  NOT NULL
                      CHECK (categoria IN ('HARDWARE', 'SOFTWARE', 'RED', 'CUENTA', 'OTRO')),
    prioridad         VARCHAR(10)  NOT NULL DEFAULT 'MEDIO'
                      CHECK (prioridad IN ('BAJO', 'MEDIO', 'ALTO', 'CRITICO')),
    estado            VARCHAR(12)  NOT NULL DEFAULT 'ABIERTO'
                      CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'RESUELTO', 'CERRADO')),
    -- username del sga-principal (quien reporta y quien atiende)
    creado_por        VARCHAR(50)  NOT NULL,
    asignado_a        VARCHAR(50),
    solucion_aplicada TEXT,
    fecha_creacion    TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_resolucion  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_estado    ON sga_soporte.tickets (estado);
CREATE INDEX IF NOT EXISTS idx_tickets_prioridad ON sga_soporte.tickets (prioridad);
CREATE INDEX IF NOT EXISTS idx_tickets_creado    ON sga_soporte.tickets (creado_por);

-- Comentarios / seguimiento de cada ticket.
CREATE TABLE IF NOT EXISTS sga_soporte.comentarios (
    id_comentario  BIGSERIAL PRIMARY KEY,
    id_ticket      BIGINT      NOT NULL REFERENCES sga_soporte.tickets (id_ticket) ON DELETE CASCADE,
    autor          VARCHAR(50) NOT NULL,
    contenido      TEXT        NOT NULL,
    nota_interna   BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_comentarios_ticket ON sga_soporte.comentarios (id_ticket);
