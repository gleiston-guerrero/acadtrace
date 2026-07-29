-- V5: Crea el esquema sga_secretaria completo (Estudiante, Representante,
-- Matricula, FichaEstudiante, HistorialPromocion, DocumentosMatricula).
--
-- Por que existe este script: sga-secretaria tiene su propio esquema de base
-- de datos (principio acordado explicitamente: "sga-secretaria debe tener su
-- propio esquema y comunicarse solo por gRPC"). sga-principal es quien
-- realmente escribe estas tablas via JPA (@Table(schema = "sga_secretaria")
-- en Estudiante/Representante/Matricula/FichaEstudiante); sga-secretaria
-- nunca hace SQL directo contra ellas.
--
-- Este esquema se habia creado a mano, directo contra una base de pruebas
-- local, sin guardar el script en el repo, por eso no aparecia en la base
-- compartida del equipo. Este archivo es exactamente esa estructura
-- (extraida con pg_dump --schema-only de la base donde ya estaba probada),
-- ya con las correcciones de las migraciones 001-004 de SGA-Secretaria
-- incluidas (esquema de representantes.telefono_principal ampliado a TEXT,
-- y las FK de matriculas/historial_promocion/fichas_estudiante apuntando a
-- sga_secretaria.estudiantes en vez de la copia congelada de sga_principal).
--
-- Requiere que sga_principal ya exista (usuarios, grados, paralelos,
-- anos_lectivos, y los tipos enum tipo_documento_t/estado_matricula_t/
-- resultado_promocion_t), por las FK cruzadas hacia ese esquema.
--
-- Como correrla: psql "$DATABASE_URL" -f sql/V5__esquema_sga_secretaria.sql

CREATE SCHEMA IF NOT EXISTS sga_secretaria;

-- ---------------------------------------------------------------------------
-- 1. Estudiante
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.estudiantes (
    id_estudiante        SERIAL PRIMARY KEY,
    cedula               VARCHAR(10) UNIQUE,
    codigo_estudiante    VARCHAR(20) UNIQUE,
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    fecha_nacimiento     DATE,
    genero               VARCHAR(10),
    direccion            TEXT,
    telefono             TEXT,
    telefono_alt         VARCHAR(20),
    correo               VARCHAR(150),
    discapacidad         BOOLEAN NOT NULL DEFAULT FALSE,
    tipo_discapacidad    TEXT,
    porcentaje_disc      SMALLINT,
    CONSTRAINT porcentaje_disc_check CHECK (porcentaje_disc >= 0 AND porcentaje_disc <= 100),
    id_representante     INTEGER,
    origen_listado       VARCHAR(50),
    estado               VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    foto_url             VARCHAR(255),
    creado_por           INTEGER REFERENCES sga_principal.usuarios (id_usuario),
    fecha_creacion       TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
    carnet_conadis       VARCHAR(30),
    nacionalidad         VARCHAR(50),
    etnia                VARCHAR(50),
    lugar_nacimiento     VARCHAR(150),
    vive_con             VARCHAR(50),
    numeros_hermanos     SMALLINT,
    beneficio_social     BOOLEAN DEFAULT FALSE
);
COMMENT ON COLUMN sga_secretaria.estudiantes.origen_listado IS 'Tipo de ingreso del estudiante: NUEVO, TRANSFERIDO, REPITENTE, REINGRESO';
CREATE INDEX IF NOT EXISTS idx_estudiantes_cedula ON sga_secretaria.estudiantes USING btree (cedula);
CREATE INDEX IF NOT EXISTS idx_estudiantes_apellidos ON sga_secretaria.estudiantes USING btree (apellidos, nombres);

-- ---------------------------------------------------------------------------
-- 2. Representante
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.representantes (
    id_representante     SERIAL PRIMARY KEY,
    cedula               VARCHAR(10),
    nombres              VARCHAR(100) NOT NULL,
    apellidos            VARCHAR(100) NOT NULL,
    parentesco           VARCHAR(50) NOT NULL,
    telefono_principal   TEXT,
    telefono_alt         VARCHAR(20),
    correo               VARCHAR(150),
    direccion            TEXT,
    fecha_creacion       TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE sga_secretaria.estudiantes
    ADD CONSTRAINT fk_estudiante_representante
        FOREIGN KEY (id_representante) REFERENCES sga_secretaria.representantes (id_representante);

-- ---------------------------------------------------------------------------
-- 3. Matricula
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.matriculas (
    id_matricula     SERIAL PRIMARY KEY,
    id_estudiante    INTEGER NOT NULL REFERENCES sga_secretaria.estudiantes (id_estudiante),
    id_grado         INTEGER NOT NULL REFERENCES sga_principal.grados (id_grado),
    id_paralelo      INTEGER NOT NULL REFERENCES sga_principal.paralelos (id_paralelo),
    id_ano_lectivo   INTEGER NOT NULL REFERENCES sga_principal.anos_lectivos (id_ano_lectivo),
    numero_orden     SMALLINT,
    fecha_registro   DATE NOT NULL DEFAULT CURRENT_DATE,
    estado           sga_principal.estado_matricula_t NOT NULL DEFAULT 'ACTIVA',
    observaciones    TEXT,
    registrado_por   INTEGER REFERENCES sga_principal.usuarios (id_usuario),
    fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT matriculas_id_estudiante_id_ano_lectivo_key UNIQUE (id_estudiante, id_ano_lectivo)
);
COMMENT ON COLUMN sga_secretaria.matriculas.id_paralelo IS 'Identifica el aula/paralelo especifico del estudiante en el ano lectivo';
CREATE INDEX IF NOT EXISTS idx_matriculas_estudiante ON sga_secretaria.matriculas USING btree (id_estudiante);
CREATE INDEX IF NOT EXISTS idx_matriculas_grado ON sga_secretaria.matriculas USING btree (id_grado);
CREATE INDEX IF NOT EXISTS idx_matriculas_paralelo ON sga_secretaria.matriculas USING btree (id_paralelo, id_ano_lectivo);
CREATE INDEX IF NOT EXISTS idx_matriculas_ano_lectivo ON sga_secretaria.matriculas USING btree (id_ano_lectivo);

-- ---------------------------------------------------------------------------
-- 4. FichaEstudiante
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.fichas_estudiante (
    id_ficha                  SERIAL PRIMARY KEY,
    id_estudiante             INTEGER NOT NULL UNIQUE
                                   REFERENCES sga_secretaria.estudiantes (id_estudiante) ON DELETE CASCADE,
    tipo_sangre               VARCHAR(5),
    alergias                  TEXT,
    medicacion_permanente     TEXT,
    enfermedad_catastrofica   BOOLEAN NOT NULL DEFAULT FALSE,
    detalle_enfermedad        TEXT,
    contacto_emergencia       VARCHAR(100),
    telefono_emergencia       VARCHAR(20),
    direccion_referencia      TEXT,
    fecha_actualizacion       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- 5. HistorialPromocion (registro de cierre de ano lectivo)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.historial_promocion (
    id_historial      SERIAL PRIMARY KEY,
    id_matricula      INTEGER NOT NULL,
    id_estudiante     INTEGER NOT NULL,
    id_grado_origen   INTEGER NOT NULL,
    id_ano_lectivo    INTEGER NOT NULL,
    resultado         sga_principal.resultado_promocion_t NOT NULL,
    promedio_anual    NUMERIC(4, 2),
    observaciones     TEXT,
    registrado_por    INTEGER,
    fecha_registro    TIMESTAMPTZ NOT NULL DEFAULT now(),
    lamport_ts        BIGINT,
    CONSTRAINT historial_matricula_unique UNIQUE (id_matricula),
    CONSTRAINT historial_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_secretaria.matriculas (id_matricula),
    CONSTRAINT historial_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes (id_estudiante),
    CONSTRAINT historial_id_grado_origen_fkey FOREIGN KEY (id_grado_origen) REFERENCES sga_principal.grados (id_grado),
    CONSTRAINT historial_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos (id_ano_lectivo),
    CONSTRAINT historial_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios (id_usuario)
);
COMMENT ON TABLE sga_secretaria.historial_promocion IS 'Registro permanente del resultado de cada estudiante al cierre de ano lectivo';
CREATE INDEX IF NOT EXISTS idx_historial_estudiante ON sga_secretaria.historial_promocion USING btree (id_estudiante, id_ano_lectivo);
CREATE INDEX IF NOT EXISTS idx_historial_promocion_lamport_ts ON sga_secretaria.historial_promocion USING btree (lamport_ts);

-- ---------------------------------------------------------------------------
-- 6. DocumentosMatricula (RF-14, adjuntos digitales)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sga_secretaria.documentos_matricula (
    id_documento      SERIAL PRIMARY KEY,
    id_matricula      INTEGER NOT NULL REFERENCES sga_secretaria.matriculas (id_matricula) ON DELETE CASCADE,
    tipo_documento    sga_principal.tipo_documento_t NOT NULL,
    nombre_archivo    VARCHAR(200) NOT NULL,
    ruta_archivo      VARCHAR(500) NOT NULL,
    fecha_subida      TIMESTAMPTZ NOT NULL DEFAULT now(),
    subido_por        INTEGER REFERENCES sga_principal.usuarios (id_usuario)
);
COMMENT ON COLUMN sga_secretaria.documentos_matricula.tipo_documento IS 'Catalogo cerrado de documentos requeridos en el proceso de matricula';
