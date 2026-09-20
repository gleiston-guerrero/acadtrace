-- Datos sinteticos y reproducibles para E10 Playwright.
-- Este archivo se ejecuta exclusivamente sobre la PostgreSQL efimera
-- creada por el job "E10 - Playwright Frontend Docente".

BEGIN;

-- Usuario docente sintetico.
INSERT INTO sga_principal.usuarios (
    id_usuario,
    username,
    correo,
    password_hash,
    primer_ingreso,
    estado
) VALUES (
    900001,
    'docente.e10@acadtrace.test',
    'docente.e10@acadtrace.test',
    :'e2e_docente_hash',
    false,
    true
);

INSERT INTO sga_principal.personas (
    id_persona,
    id_usuario,
    cedula,
    nombres,
    apellidos,
    correo_personal,
    cargo
) VALUES (
    900001,
    900001,
    '9999000001',
    'Docente',
    'Sintetico E10',
    'docente.e10@acadtrace.test',
    'DOCENTE'
);

INSERT INTO sga_principal.roles (
    id_rol,
    nombre,
    descripcion,
    activo
) VALUES (
    900001,
    'ROLE_DOCENTE',
    'Rol sintetico exclusivo de E10',
    true
);

INSERT INTO sga_principal.usuario_roles (
    id_usuario,
    id_rol
) VALUES (
    900001,
    900001
);

-- Ano lectivo sintetico.
INSERT INTO sga_principal.anos_lectivos (
    id_ano_lectivo,
    nombre,
    fecha_inicio,
    fecha_fin,
    es_actual
) VALUES (
    900001,
    'E10-2026',
    DATE '2026-01-01',
    DATE '2026-12-31',
    true
);

-- Estructura academica sintetica.
INSERT INTO sga_principal.niveles_educativos (
    id_nivel,
    nombre,
    tipo_escala,
    grado_inicio,
    grado_fin
) VALUES (
    900001,
    'Nivel Sintetico E10',
    'CUANTITATIVA',
    1,
    10
);

INSERT INTO sga_principal.grados (
    id_grado,
    id_nivel,
    nombre,
    orden,
    capacidad_max,
    activo
) VALUES (
    900001,
    900001,
    'Grado E10',
    1,
    30,
    true
);

INSERT INTO sga_principal.paralelos (
    id_paralelo,
    id_grado,
    letra,
    activo
) VALUES (
    900001,
    900001,
    'A',
    true
);

INSERT INTO sga_principal.paralelos_ano_lectivo (
    id_paralelo_al,
    id_paralelo,
    id_ano_lectivo,
    capacidad_max,
    activo
) VALUES (
    900001,
    900001,
    900001,
    30,
    true
);

INSERT INTO sga_principal.asignaturas (
    id_asignatura,
    nombre,
    codigo,
    descripcion,
    horas_semana,
    activa
) VALUES (
    900001,
    'Matematica E10',
    'E10-MAT',
    'Asignatura sintetica exclusiva de Playwright E10',
    5,
    true
);

-- Tablas y columnas requeridas por entidades JPA de sga_principal
ALTER TABLE sga_principal.asignaciones
    ADD COLUMN IF NOT EXISTS horas_semanales integer DEFAULT 4 NOT NULL;

ALTER TABLE sga_principal.horarios
    ADD COLUMN IF NOT EXISTS hora_inicio time DEFAULT '07:30' NOT NULL,
    ADD COLUMN IF NOT EXISTS hora_fin time DEFAULT '08:15' NOT NULL,
    ADD COLUMN IF NOT EXISTS aula character varying(50),
    ADD COLUMN IF NOT EXISTS id_periodo integer;

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

INSERT INTO sga_principal.asignaciones (
    id_asignacion,
    id_docente,
    id_asignatura,
    id_grado,
    id_paralelo,
    id_ano_lectivo,
    es_tutor,
    activo,
    horas_semanales
) VALUES (
    900001,
    900001,
    900001,
    900001,
    900001,
    900001,
    true,
    true,
    4
) ON CONFLICT (id_asignacion) DO UPDATE SET
    id_docente = EXCLUDED.id_docente,
    id_asignatura = EXCLUDED.id_asignatura,
    id_grado = EXCLUDED.id_grado,
    id_paralelo = EXCLUDED.id_paralelo,
    id_ano_lectivo = EXCLUDED.id_ano_lectivo,
    es_tutor = EXCLUDED.es_tutor,
    activo = EXCLUDED.activo,
    horas_semanales = EXCLUDED.horas_semanales;

-- Vistas de compatibilidad en sga_principal para entidades JPA y servicios
CREATE OR REPLACE VIEW sga_principal.estudiantes AS
    SELECT * FROM sga_secretaria.estudiantes;

CREATE OR REPLACE VIEW sga_principal.matriculas AS
    SELECT * FROM sga_secretaria.matriculas;

CREATE OR REPLACE VIEW sga_principal.fichas_estudiante AS
    SELECT * FROM sga_secretaria.fichas_estudiante;

-- Estudiante sintetico (almacenado en sga_secretaria y expuesto en sga_principal).
INSERT INTO sga_secretaria.estudiantes (
    id_estudiante,
    cedula,
    codigo_estudiante,
    nombres,
    apellidos,
    estado
) VALUES (
    900001,
    '9999000002',
    'E10-EST-001',
    'Estudiante',
    'Sintetico E10',
    'ACTIVO'
) ON CONFLICT (id_estudiante) DO UPDATE SET
    cedula = EXCLUDED.cedula,
    nombres = EXCLUDED.nombres,
    apellidos = EXCLUDED.apellidos,
    estado = EXCLUDED.estado;

INSERT INTO sga_secretaria.matriculas (
    id_matricula,
    id_estudiante,
    id_grado,
    id_paralelo,
    id_ano_lectivo,
    numero_orden,
    estado
) VALUES (
    900001,
    900001,
    900001,
    900001,
    900001,
    1,
    'ACTIVA'
) ON CONFLICT (id_matricula) DO UPDATE SET
    id_estudiante = EXCLUDED.id_estudiante,
    id_grado = EXCLUDED.id_grado,
    id_paralelo = EXCLUDED.id_paralelo,
    id_ano_lectivo = EXCLUDED.id_ano_lectivo,
    estado = EXCLUDED.estado;

-- Conceder permisos al rol de aplicacion sga_app sobre sga_secretaria y sga_docente si existen
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sga_app') THEN
        IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sga_secretaria') THEN
            GRANT USAGE ON SCHEMA sga_secretaria TO sga_app;
            GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_secretaria TO sga_app;
            GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_secretaria TO sga_app;
        END IF;
        IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'sga_docente') THEN
            GRANT USAGE ON SCHEMA sga_docente TO sga_app;
            GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA sga_docente TO sga_app;
            GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA sga_docente TO sga_app;
        END IF;
    END IF;
END $$;

COMMIT;