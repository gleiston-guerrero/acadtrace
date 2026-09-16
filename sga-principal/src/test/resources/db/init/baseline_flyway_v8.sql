-- =============================================================================
-- Baseline Real de Producción V8 (AcadTrace - PostgreSQL 15+)
--
-- Representa la línea base completa del esquema de base de datos productiva
-- consolidada hasta la versión histórica V8 previa a la integración de Flyway.
-- A partir de esta línea base, el pipeline real de Flyway aplica las
-- migraciones versionadas V9 hasta V18.
--
-- El disparador de inmutabilidad tg_auditoria_append_only NO se crea aquí:
-- es creado exclusivamente por la migración productiva V13.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Esquemas del sistema distribuido
CREATE SCHEMA IF NOT EXISTS sga_principal;
CREATE SCHEMA IF NOT EXISTS secretaria;
CREATE SCHEMA IF NOT EXISTS docente;
CREATE SCHEMA IF NOT EXISTS soporte;

-- 2. Enumeraciones de dominio nativas
DO $$ BEGIN
    CREATE TYPE sga_principal.accion_auditoria_t AS ENUM (
        'CREAR', 'EDITAR', 'ELIMINAR', 'LOGIN', 'LOGOUT',
        'CAMBIO_PASSWORD', 'BLOQUEO', 'DESBLOQUEO',
        'LOGIN_EXITOSO', 'LOGIN_FALLIDO',
        'ROL_ASIGNADO', 'LLAMADA_GRPC', 'CONSULTAR',
        'MODIFICAR', 'INSERT'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.estado_matricula_t AS ENUM (
        'ACTIVA', 'RETIRADA', 'TRASLADADA', 'PROMOVIDA', 'REPROBADA'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.genero_t AS ENUM (
        'MASCULINO', 'FEMENINO', 'OTRO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.dia_semana_t AS ENUM (
        'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.nivel_educativo_t AS ENUM (
        'INICIAL_1', 'INICIAL_2', 'PREPARATORIA',
        'BASICA_ELEMENTAL', 'BASICA_MEDIA', 'BASICA_SUPERIOR'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.origen_listado_t AS ENUM (
        'NUEVO', 'TRANSFERIDO_INTERNO', 'TRANSFERIDO_EXTERNO',
        'REPITENTE', 'REINGRESO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.resultado_promocion_t AS ENUM (
        'PROMOVIDO', 'REPROBADO', 'RETIRADO', 'TRASLADADO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sga_principal.tipo_asignacion_t AS ENUM (
        'TITULAR', 'ESPECIALIZADO'
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 3. Tablas principales de la línea base V8

CREATE TABLE IF NOT EXISTS sga_principal.roles (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sga_principal.usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE,
    correo VARCHAR(255) UNIQUE,
    email VARCHAR(150),
    password_hash VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.usuario_roles (
    id_usuario BIGINT NOT NULL REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE,
    id_rol BIGINT NOT NULL REFERENCES sga_principal.roles(id_rol) ON DELETE CASCADE,
    PRIMARY KEY (id_usuario, id_rol)
);

CREATE TABLE IF NOT EXISTS sga_principal.estudiantes (
    id_estudiante BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(255) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE,
    genero VARCHAR(20),
    direccion TEXT,
    id_usuario BIGINT REFERENCES sga_principal.usuarios(id_usuario),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.representantes (
    id_representante BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(255),
    nombres VARCHAR(100),
    apellidos VARCHAR(100),
    telefono VARCHAR(255),
    email VARCHAR(255),
    correo VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sga_principal.grados (
    id_grado BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    nivel VARCHAR(50),
    paralelo VARCHAR(5),
    capacidad_maxima INTEGER NOT NULL DEFAULT 35,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sga_principal.periodos_academicos (
    id_periodo BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    fecha_inicio DATE,
    fecha_fin DATE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sga_principal.matriculas (
    id_matricula BIGSERIAL PRIMARY KEY,
    id_estudiante BIGINT NOT NULL REFERENCES sga_principal.estudiantes(id_estudiante),
    id_grado BIGINT NOT NULL REFERENCES sga_principal.grados(id_grado),
    id_periodo BIGINT NOT NULL REFERENCES sga_principal.periodos_academicos(id_periodo),
    numero_matricula VARCHAR(50) UNIQUE NOT NULL,
    folio VARCHAR(50),
    estado VARCHAR(30) NOT NULL DEFAULT 'MATRICULADO',
    fecha_matricula TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.calificaciones (
    id_calificacion BIGSERIAL PRIMARY KEY,
    id_matricula BIGINT NOT NULL REFERENCES sga_principal.matriculas(id_matricula),
    materia VARCHAR(100) NOT NULL,
    nota_formativa NUMERIC(4,2),
    nota_sumativa NUMERIC(4,2),
    promedio_final NUMERIC(4,2),
    estado VARCHAR(20) DEFAULT 'REGISTRADA',
    reloj_lamport BIGINT NOT NULL DEFAULT 1,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.eventos_academicos (
    id_evento BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    tipo VARCHAR(30),
    id_grado BIGINT REFERENCES sga_principal.grados(id_grado),
    creado_por BIGINT REFERENCES sga_principal.usuarios(id_usuario),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. Bitácora de auditoría centralizada en la línea base V8 (antes del disparador V13)
CREATE TABLE IF NOT EXISTS sga_principal.auditoria (
    id_auditoria BIGSERIAL PRIMARY KEY,
    schema_origen VARCHAR(50) NOT NULL,
    trace_id UUID NOT NULL DEFAULT gen_random_uuid(),
    username VARCHAR(50),
    accion sga_principal.accion_auditoria_t NOT NULL,
    tabla_afectada VARCHAR(150) NOT NULL,
    registro_id BIGINT,
    descripcion TEXT,
    ip_address VARCHAR(45),
    resultado VARCHAR(50) NOT NULL DEFAULT 'EXITO',
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_auditoria_trace ON sga_principal.auditoria (trace_id);
CREATE INDEX IF NOT EXISTS ix_auditoria_schema ON sga_principal.auditoria (schema_origen);
CREATE INDEX IF NOT EXISTS ix_auditoria_fecha ON sga_principal.auditoria (fecha);

-- 5. Esquema soporte en la línea base
CREATE TABLE IF NOT EXISTS soporte.tickets (
    id_ticket BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT NOT NULL,
    categoria VARCHAR(50) NOT NULL,
    prioridad VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    usuario_reporta VARCHAR(50) NOT NULL,
    tecnico_asignado VARCHAR(50),
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_cierre TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS soporte.ticket_comentarios (
    id_comentario BIGSERIAL PRIMARY KEY,
    id_ticket BIGINT NOT NULL REFERENCES soporte.tickets(id_ticket) ON DELETE CASCADE,
    autor VARCHAR(50) NOT NULL,
    comentario TEXT NOT NULL,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);