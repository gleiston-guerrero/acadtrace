-- Tabla de eventos academicos globales (calendario).
--
-- El CRUD vive en microservicio-secretaria (CalendarioController), pero la
-- tabla se crea en el esquema sga_principal porque el calendario es un dato
-- institucional compartido por los 3 portales (secretaria/docente/soporte),
-- igual criterio que sga_principal.historial_promocion (escrito directo por
-- SQL desde ese microservicio, sin pasar por gRPC).
--
-- Como correrlo:
-- Suministrar PGPASSWORD desde el entorno antes de ejecutar psql; no guardar la clave en el repositorio.
-- psql -h 192.0.2.1 -p 5433 -U postgres -d sga \
--   -v ON_ERROR_STOP=1 -f scripts/create_eventos_academicos.sql

CREATE TABLE IF NOT EXISTS sga_principal.eventos_academicos (
    id_evento       SERIAL PRIMARY KEY,
    titulo          VARCHAR(150) NOT NULL,
    descripcion     TEXT,
    fecha_inicio    DATE NOT NULL,
    fecha_fin       DATE,
    tipo            VARCHAR(30), -- REUNION, EVALUACION, FERIADO, CIVICO
    id_grado        INTEGER REFERENCES sga_principal.grados (id_grado),
    creado_por      INTEGER,
    fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_eventos_academicos_fecha
    ON sga_principal.eventos_academicos (fecha_inicio, fecha_fin);
