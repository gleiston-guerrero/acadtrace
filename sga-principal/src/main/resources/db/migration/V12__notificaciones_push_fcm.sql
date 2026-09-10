CREATE TABLE IF NOT EXISTS sga_principal.dispositivos_representante (
    id_dispositivo BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    plataforma VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_dispositivo_usuario_activo
    ON sga_principal.dispositivos_representante(id_usuario, activo);

CREATE TABLE IF NOT EXISTS sga_principal.eventos_notificacion_push (
    id_evento BIGSERIAL PRIMARY KEY,
    clave_evento VARCHAR(180) NOT NULL,
    id_usuario BIGINT NOT NULL REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_envio TIMESTAMPTZ,
    CONSTRAINT uq_evento_push_destinatario UNIQUE (clave_evento, id_usuario)
);
