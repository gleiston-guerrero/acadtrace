-- =============================================================================
-- V25__delta_v8_tablas_vistas.sql
--
-- Proposito: reponer hacia adelante los objetos que se habian anadido
-- editando V8__baseline_completo.sql despues de aplicado (lo que rompia el
-- checksum de Flyway en bases ya migradas). V8 quedo restaurado a su estado
-- oficial del commit 92f2ec91 y este archivo versionado nuevo contiene
-- exactamente ese delta, sin modificar ninguna migracion aplicada (V8..V24).
--
-- Autor: Pedro Castro. Fecha: 2026-09-21. Punto 5 del PFC, equipo BCEL.
--
-- Todo es idempotente: CREATE TABLE/INDEX/SEQUENCE IF NOT EXISTS,
-- ADD COLUMN IF NOT EXISTS, CREATE OR REPLACE VIEW y bloques DO con guarda
-- IF NOT EXISTS sobre pg_constraint. Reejecutable sin efecto adverso.
-- Sin secretos, sin literales de contrasena, sin IPs.
-- =============================================================================

-- =============================================================================
-- Vistas de compatibilidad en sga_principal para entidades JPA y consultas
-- que leen estudiantes y matriculas desde sga_principal manteniendo sga_secretaria como fuente.
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS sga_secretaria;

CREATE TABLE IF NOT EXISTS sga_secretaria.estudiantes (
    id_estudiante serial PRIMARY KEY,
    cedula character varying(10),
    codigo_estudiante character varying(20),
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    fecha_nacimiento date,
    genero character varying(10),
    direccion text,
    telefono character varying(20),
    telefono_alt character varying(20),
    correo character varying(150),
    discapacidad boolean DEFAULT false NOT NULL,
    tipo_discapacidad character varying(100),
    porcentaje_disc smallint,
    id_representante integer,
    origen_listado character varying(50),
    estado character varying(20) DEFAULT 'ACTIVO'::character varying NOT NULL,
    foto_url character varying(255),
    creado_por integer,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL,
    carnet_conadis character varying(30),
    nacionalidad character varying(50),
    etnia character varying(50),
    lugar_nacimiento character varying(150),
    vive_con character varying(50),
    numeros_hermanos smallint,
    beneficio_social boolean DEFAULT false
);

CREATE TABLE IF NOT EXISTS sga_secretaria.matriculas (
    id_matricula serial PRIMARY KEY,
    id_estudiante integer NOT NULL,
    id_grado integer NOT NULL,
    id_paralelo integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    numero_orden smallint,
    fecha_registro date DEFAULT CURRENT_DATE NOT NULL,
    estado character varying(20) DEFAULT 'ACTIVA'::character varying NOT NULL,
    observaciones text
);

CREATE TABLE IF NOT EXISTS sga_secretaria.fichas_estudiante (
    id_ficha serial PRIMARY KEY,
    id_estudiante integer NOT NULL,
    tipo_sangre character varying(5),
    alergias text,
    medicacion_permanente text,
    enfermedad_catastrofica boolean DEFAULT false NOT NULL,
    detalle_enfermedad text,
    contacto_emergencia character varying(100),
    telefono_emergencia character varying(20),
    direccion_referencia text,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE OR REPLACE VIEW sga_principal.estudiantes AS
    SELECT * FROM sga_secretaria.estudiantes;

CREATE OR REPLACE VIEW sga_principal.matriculas AS
    SELECT * FROM sga_secretaria.matriculas;

CREATE OR REPLACE VIEW sga_principal.fichas_estudiante AS
    SELECT * FROM sga_secretaria.fichas_estudiante;

-- =============================================================================
-- Tablas y columnas requeridas por entidades JPA de sga_principal (Horario, Asignacion, Malla, etc.)
-- =============================================================================
ALTER TABLE sga_principal.asignaciones
    ADD COLUMN IF NOT EXISTS horas_semanales integer DEFAULT 4 NOT NULL;

ALTER TABLE sga_principal.horarios
    ADD COLUMN IF NOT EXISTS hora_inicio time DEFAULT '07:30' NOT NULL,
    ADD COLUMN IF NOT EXISTS hora_fin time DEFAULT '08:15' NOT NULL,
    ADD COLUMN IF NOT EXISTS aula character varying(50),
    ADD COLUMN IF NOT EXISTS id_periodo integer;

-- Valores por omision que el nuevo esquema fija sobre columnas preexistentes.
ALTER TABLE sga_principal.horarios
    ALTER COLUMN id_periodo_diario SET DEFAULT 1;
ALTER TABLE sga_principal.horarios
    ALTER COLUMN dia_semana SET DEFAULT 'LUNES'::sga_principal.dia_semana_t;

CREATE TABLE IF NOT EXISTS sga_principal.periodos_horario (
    id_periodo serial PRIMARY KEY,
    nombre varchar(30) NOT NULL,
    hora_inicio time NOT NULL,
    hora_fin time NOT NULL,
    orden int NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_periodo_orden UNIQUE (orden),
    CONSTRAINT ck_periodo_rango CHECK (hora_inicio < hora_fin)
);

CREATE TABLE IF NOT EXISTS sga_principal.malla_curricular (
    id_malla serial PRIMARY KEY,
    id_grado integer NOT NULL,
    id_asignatura integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    horas_semana smallint NOT NULL,
    dias_semana smallint,
    duracion smallint,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.periodos_evaluacion (
    id_periodo serial PRIMARY KEY,
    id_ano_lectivo integer NOT NULL,
    tipo varchar(20) NOT NULL,
    nombre varchar(100) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.esquema_calificacion (
    id_esquema serial PRIMARY KEY,
    id_ano_lectivo integer NOT NULL,
    peso_formativa numeric(5,2) DEFAULT 70.00 NOT NULL,
    peso_sumativa numeric(5,2) DEFAULT 30.00 NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.tipos_aporte (
    id_tipo_aporte serial PRIMARY KEY,
    id_ano_lectivo integer NOT NULL,
    nombre varchar(60) NOT NULL,
    tipo_evaluacion varchar(12) DEFAULT 'FORMATIVA' NOT NULL,
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL
);

CREATE TABLE IF NOT EXISTS sga_principal.escala_calificaciones (
    id_escala serial PRIMARY KEY,
    id_ano_lectivo integer NOT NULL,
    id_nivel integer NOT NULL,
    nota_minima numeric(4,2) NOT NULL,
    nota_maxima numeric(4,2) NOT NULL,
    equivalente_cualitativo varchar(5),
    descripcion varchar(100)
);

-- =============================================================================
-- Representantes: columnas requeridas por Representante.java y microservicio-secretaria
-- =============================================================================
ALTER TABLE sga_principal.representantes
    ADD COLUMN IF NOT EXISTS id_usuario BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_nacimiento date,
    ADD COLUMN IF NOT EXISTS genero varchar(20),
    ADD COLUMN IF NOT EXISTS estado_civil varchar(30),
    ADD COLUMN IF NOT EXISTS nacionalidad varchar(50),
    ADD COLUMN IF NOT EXISTS ocupacion varchar(100),
    ADD COLUMN IF NOT EXISTS lugar_trabajo varchar(150),
    ADD COLUMN IF NOT EXISTS telefono_trabajo varchar(20),
    ADD COLUMN IF NOT EXISTS cargo varchar(100),
    ADD COLUMN IF NOT EXISTS nivel_instruccion varchar(50),
    ADD COLUMN IF NOT EXISTS ingreso_mensual numeric(10,2),
    ADD COLUMN IF NOT EXISTS convive_con_estudiante boolean,
    ADD COLUMN IF NOT EXISTS contacto_emergencia_nombre varchar(150),
    ADD COLUMN IF NOT EXISTS contacto_emergencia_telefono varchar(20),
    ADD COLUMN IF NOT EXISTS observaciones text;

ALTER TABLE sga_secretaria.representantes
    ADD COLUMN IF NOT EXISTS fecha_nacimiento date,
    ADD COLUMN IF NOT EXISTS genero varchar(20),
    ADD COLUMN IF NOT EXISTS estado_civil varchar(30),
    ADD COLUMN IF NOT EXISTS nacionalidad varchar(50),
    ADD COLUMN IF NOT EXISTS ocupacion varchar(100),
    ADD COLUMN IF NOT EXISTS lugar_trabajo varchar(150),
    ADD COLUMN IF NOT EXISTS telefono_trabajo varchar(20),
    ADD COLUMN IF NOT EXISTS cargo varchar(100),
    ADD COLUMN IF NOT EXISTS nivel_instruccion varchar(50),
    ADD COLUMN IF NOT EXISTS ingreso_mensual numeric(10,2),
    ADD COLUMN IF NOT EXISTS convive_con_estudiante boolean,
    ADD COLUMN IF NOT EXISTS contacto_emergencia_nombre varchar(150),
    ADD COLUMN IF NOT EXISTS contacto_emergencia_telefono varchar(20),
    ADD COLUMN IF NOT EXISTS observaciones text;

-- =============================================================================
-- Tablas de notificaciones y auditoria requeridas por entidades JPA de sga_principal
-- =============================================================================
CREATE TABLE IF NOT EXISTS sga_principal.notificaciones (
    id_notificacion BIGSERIAL PRIMARY KEY,
    id_usuario INTEGER REFERENCES sga_principal.usuarios (id_usuario),
    tipo VARCHAR(30) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT,
    url_destino VARCHAR(255),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sga_principal.dispositivos_representante (
    id_dispositivo BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL REFERENCES sga_principal.usuarios(id_usuario) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    plataforma VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now()
);

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

ALTER TABLE sga_principal.auditoria
    ADD COLUMN IF NOT EXISTS contenido_canonico TEXT,
    ADD COLUMN IF NOT EXISTS version_canonica VARCHAR(20);

CREATE TABLE IF NOT EXISTS sga_principal.estado_cadena_auditoria (
    id_estado SMALLINT PRIMARY KEY,
    ultimo_hash VARCHAR(64) NOT NULL,
    ultimo_lamport BIGINT NOT NULL DEFAULT 0,
    vector_reloj TEXT,
    CONSTRAINT ck_estado_cadena_auditoria_singleton CHECK (id_estado = 1)
);
