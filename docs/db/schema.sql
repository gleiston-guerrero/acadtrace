-- =============================================================================
-- AcadTrace - Definicion de Esquema de Base de Datos Multi-Esquema
-- PostgreSQL 15+ / Proyecto Fin de Curso (Entrega 4)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. CREACION DE ESQUEMAS AISLADOS POR DOMINIO (Particionamiento Logico)
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS sga_principal;
CREATE SCHEMA IF NOT EXISTS secretaria;
CREATE SCHEMA IF NOT EXISTS docente;
CREATE SCHEMA IF NOT EXISTS soporte;

-- -----------------------------------------------------------------------------
-- 2. TIPOS PERSONALIZADOS Y ENUMERACIONES
-- -----------------------------------------------------------------------------
DO $$ BEGIN
    CREATE TYPE sga_principal.accion_auditoria_t AS ENUM (
        'CREAR', 'MODIFICAR', 'ELIMINAR', 'LOGIN_EXITOSO', 'LOGIN_FALLIDO',
        'ROL_ASIGNADO', 'LLAMADA_GRPC', 'CONSULTAR'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- -----------------------------------------------------------------------------
-- 3. ESQUEMA sga_principal: NUCLEO ACADEMICO Y BITACORA CRIPTOGRAFICA
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sga_principal.usuarios (
    id_usuario          SERIAL PRIMARY KEY,
    username            VARCHAR(50) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    email               VARCHAR(150) UNIQUE NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.roles (
    id_rol              SERIAL PRIMARY KEY,
    nombre              VARCHAR(50) UNIQUE NOT NULL,
    descripcion         TEXT
);

CREATE TABLE IF NOT EXISTS sga_principal.usuario_roles (
    id_usuario          INTEGER NOT NULL REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE,
    id_rol              INTEGER NOT NULL REFERENCES sga_principal.roles(id_rol) ON DELETE CASCADE,
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE IF NOT EXISTS sga_principal.estudiantes (
    id_estudiante       SERIAL PRIMARY KEY,
    cedula              VARCHAR(255) NOT NULL, -- Cifrado AES-256-GCM
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    fecha_nacimiento    DATE NOT NULL,
    genero              VARCHAR(20),
    direccion           TEXT,
    id_usuario          INTEGER REFERENCES sga_principal.usuarios(id_usuario),
    activo              BOOLEAN NOT NULL DEFAULT true,
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.representantes (
    id_representante    SERIAL PRIMARY KEY,
    cedula              VARCHAR(255) NOT NULL, -- Cifrado AES-256-GCM
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    telefono            VARCHAR(255),          -- Cifrado AES-256-GCM
    email               VARCHAR(255),
    id_usuario          INTEGER REFERENCES sga_principal.usuarios(id_usuario),
    activo              BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS sga_principal.grados (
    id_grado            SERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    nivel               VARCHAR(50) NOT NULL,
    paralelo            VARCHAR(5) NOT NULL,
    capacidad_maxima    INTEGER NOT NULL DEFAULT 35,
    activo              BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS sga_principal.periodos_academicos (
    id_periodo          SERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE NOT NULL,
    activo              BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS sga_principal.matriculas (
    id_matricula        SERIAL PRIMARY KEY,
    id_estudiante       INTEGER NOT NULL REFERENCES sga_principal.estudiantes(id_estudiante),
    id_grado            INTEGER NOT NULL REFERENCES sga_principal.grados(id_grado),
    id_periodo          INTEGER NOT NULL REFERENCES sga_principal.periodos_academicos(id_periodo),
    numero_matricula    VARCHAR(50) UNIQUE NOT NULL,
    folio               VARCHAR(50),
    estado              VARCHAR(30) NOT NULL DEFAULT 'MATRICULADO',
    fecha_matricula     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_estudiante_periodo UNIQUE (id_estudiante, id_periodo)
);

CREATE TABLE IF NOT EXISTS sga_principal.calificaciones (
    id_calificacion     SERIAL PRIMARY KEY,
    id_matricula        INTEGER NOT NULL REFERENCES sga_principal.matriculas(id_matricula),
    materia             VARCHAR(100) NOT NULL,
    nota_formativa      NUMERIC(4,2) CHECK (nota_formativa >= 0 AND nota_formativa <= 10),
    nota_sumativa       NUMERIC(4,2) CHECK (nota_sumativa >= 0 AND nota_sumativa <= 10),
    promedio_final      NUMERIC(4,2) CHECK (promedio_final >= 0 AND promedio_final <= 10),
    estado              VARCHAR(20) DEFAULT 'REGISTRADA',
    reloj_lamport       BIGINT NOT NULL DEFAULT 1,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.eventos_academicos (
    id_evento           SERIAL PRIMARY KEY,
    titulo              VARCHAR(150) NOT NULL,
    descripcion         TEXT,
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE,
    tipo                VARCHAR(30),
    id_grado            INTEGER REFERENCES sga_principal.grados(id_grado),
    creado_por          INTEGER REFERENCES sga_principal.usuarios(id_usuario),
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 4. BITACORA CRIPTOGRAFICA DE AUDITORIA (Inmutable, Append-Only)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_principal.auditoria (
    id_auditoria        SERIAL PRIMARY KEY,
    schema_origen       VARCHAR(30) NOT NULL CHECK (schema_origen IN ('PRINCIPAL', 'DOCENTE', 'SECRETARIA', 'SOPORTE')),
    trace_id            UUID NOT NULL DEFAULT gen_random_uuid(),
    username            VARCHAR(50),
    accion              sga_principal.accion_auditoria_t NOT NULL,
    tabla_afectada      VARCHAR(100),
    registro_id         BIGINT,
    descripcion         TEXT,
    ip_address          VARCHAR(45),
    resultado           VARCHAR(10) NOT NULL DEFAULT 'EXITO' CHECK (resultado IN ('EXITO', 'FALLO')),
    hmac                VARCHAR(64),           -- HMAC-SHA256 firma criptografica
    hash_anterior       VARCHAR(64),           -- SHA-256 encadenamiento blockchain-style
    reloj_lamport       BIGINT DEFAULT 1,      -- Marca logica escalar
    vector_reloj        TEXT,                  -- Version vector [N1, N2, N3]
    fecha               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_auditoria_trace ON sga_principal.auditoria (trace_id);
CREATE INDEX IF NOT EXISTS ix_auditoria_schema ON sga_principal.auditoria (schema_origen);
CREATE INDEX IF NOT EXISTS ix_auditoria_fecha ON sga_principal.auditoria (fecha);

-- Disparador de Proteccion Estricta: Prohibir UPDATE y DELETE
CREATE OR REPLACE FUNCTION sga_principal.prohibir_modificacion_auditoria()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operacion rechazada: La tabla sga_principal.auditoria es una bitacora inmutable de solo adicion (append-only) protegida bajo estandares ISO/IEC 25010';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_auditoria_append_only ON sga_principal.auditoria;

CREATE TRIGGER tg_auditoria_append_only
BEFORE UPDATE OR DELETE ON sga_principal.auditoria
FOR EACH ROW
EXECUTE FUNCTION sga_principal.prohibir_modificacion_auditoria();

-- -----------------------------------------------------------------------------
-- 5. ESQUEMA soporte: TICKETS Y ASISTENCIA TECNICA
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS soporte.tickets (
    id_ticket           SERIAL PRIMARY KEY,
    codigo              VARCHAR(30) UNIQUE NOT NULL,
    titulo              VARCHAR(150) NOT NULL,
    descripcion         TEXT NOT NULL,
    categoria           VARCHAR(50) NOT NULL,
    prioridad           VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    estado              VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    usuario_reporta     VARCHAR(50) NOT NULL,
    tecnico_asignado    VARCHAR(50),
    fecha_creacion      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cierre        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS soporte.ticket_comentarios (
    id_comentario       SERIAL PRIMARY KEY,
    id_ticket           INTEGER NOT NULL REFERENCES soporte.tickets(id_ticket) ON DELETE CASCADE,
    autor               VARCHAR(50) NOT NULL,
    comentario          TEXT NOT NULL,
    fecha               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
