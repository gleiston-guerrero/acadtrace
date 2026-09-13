-- =============================================================================
-- Baseline V8 exclusivo para probar el pipeline real de Flyway.
--
-- Representa las estructuras previas necesarias para ejecutar V9-V17.
-- El trigger de inmutabilidad NO se crea aqui:
-- debe ser creado exclusivamente por la migracion productiva V13.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS sga_principal;

CREATE TYPE sga_principal.accion_auditoria_t AS ENUM (
    'CREAR',
    'EDITAR',
    'ELIMINAR',
    'LOGIN',
    'LOGOUT',
    'CAMBIO_PASSWORD',
    'BLOQUEO',
    'DESBLOQUEO',
    'LOGIN_EXITOSO',
    'LOGIN_FALLIDO',
    'ROL_ASIGNADO',
    'LLAMADA_GRPC',
    'CONSULTAR',
    'MODIFICAR',
    'INSERT'
);

CREATE TABLE sga_principal.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE sga_principal.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    correo VARCHAR(255) UNIQUE
);

CREATE TABLE sga_principal.representantes (
    id_representante BIGSERIAL PRIMARY KEY,
    correo VARCHAR(255)
);

CREATE TABLE sga_principal.usuario_roles (
    id_usuario BIGINT NOT NULL
        REFERENCES sga_principal.usuarios(id_usuario),
    id_rol BIGINT NOT NULL
        REFERENCES sga_principal.roles(id_rol),
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE sga_principal.auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    schema_origen VARCHAR(50) NOT NULL,
    accion sga_principal.accion_auditoria_t NOT NULL,
    tabla_afectada VARCHAR(150) NOT NULL,
    registro_id BIGINT,
    descripcion TEXT,
    resultado VARCHAR(50),
    trace_id UUID
);