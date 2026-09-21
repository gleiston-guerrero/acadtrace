-- Notificaciones cross-servicio.
--
-- Tabla central de notificaciones para los 3 portales (secretaría, docente,
-- soporte). Cualquier microservicio puede insertar aquí directo por SQL
-- (mismo patrón ya usado por microservicio-secretaria para
-- sga_principal.historial_promocion) o vía POST /api/notificaciones/masivo.
--
-- Como correrlo:
-- Suministrar PGPASSWORD desde el entorno antes de ejecutar psql; no guardar la clave en el repositorio.
-- psql -h "${DB_HOST:?Configure DB_HOST}" -p 5433 -U postgres -d sga \
--   -v ON_ERROR_STOP=1 -f scripts/create_notificaciones.sql

CREATE TABLE IF NOT EXISTS sga_principal.notificaciones (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_usuario      INTEGER REFERENCES sga_principal.usuarios (id_usuario),
    tipo            VARCHAR(30)  NOT NULL,
    titulo          VARCHAR(150) NOT NULL,
    mensaje         TEXT,
    url_destino     VARCHAR(255),
    leida           BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_usuario
    ON sga_principal.notificaciones (id_usuario, leida, fecha DESC);
