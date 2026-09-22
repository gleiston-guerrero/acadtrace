-- =============================================================================
-- V26__objetos_faltantes_codigo.sql
--
-- Proposito: crear hacia adelante los objetos que el codigo usa y que no
-- existen en la linea base V8 (92f2ec91) ni en V9..V24. Sin editar ninguna
-- migracion aplicada.
--
-- Autor: Pedro Castro. Fecha: 2026-09-21. Punto 5 del PFC, equipo BCEL.
--
-- Todo es idempotente: CREATE TABLE/INDEX/SEQUENCE IF NOT EXISTS,
-- CREATE OR REPLACE VIEW, CREATE OR REPLACE FUNCTION y bloques DO con guarda
-- IF NOT EXISTS sobre pg_constraint / information_schema. Reejecutable sin
-- efecto adverso. Sin secretos, sin literales de contrasena, sin IPs.
--
-- Nota: los nombres de restriccion llevan prefijo hp_principal_ para no
-- colisionar con las restricciones homonimas de sga_secretaria.historial_promocion
-- (los nombres de restriccion son unicos en toda la base PostgreSQL).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS sga_principal;
CREATE SCHEMA IF NOT EXISTS sga_secretaria;
CREATE SCHEMA IF NOT EXISTS sga_soporte;

-- =============================================================================
-- 1. sga_principal.historial_promocion (bitacora de promocion con reloj Lamport).
--    La usa HistorialService de microservicio-secretaria con
--    SELECT COALESCE(MAX(lamport_ts), 0) FROM sga_principal.historial_promocion.
--    Estructura canonica de baseline_flyway_v8.sql de pruebas + lamport_ts de
--    la migracion 002_lamport_clock.sql de secretaria.
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS sga_principal.historial_promocion_id_historial_seq;

CREATE TABLE IF NOT EXISTS sga_principal.historial_promocion (
    id_historial integer NOT NULL,
    id_matricula integer NOT NULL,
    id_estudiante integer NOT NULL,
    id_grado_origen integer NOT NULL,
    id_ano_lectivo integer NOT NULL,
    resultado sga_principal.resultado_promocion_t NOT NULL,
    promedio_anual numeric(4,2),
    observaciones text,
    registrado_por integer,
    fecha_registro timestamp with time zone DEFAULT now() NOT NULL,
    lamport_ts bigint
);

ALTER SEQUENCE sga_principal.historial_promocion_id_historial_seq
    OWNED BY sga_principal.historial_promocion.id_historial;

ALTER TABLE ONLY sga_principal.historial_promocion
    ALTER COLUMN id_historial
    SET DEFAULT nextval('sga_principal.historial_promocion_id_historial_seq'::regclass);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_pkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_pkey PRIMARY KEY (id_historial);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_matricula_unique') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_matricula_unique UNIQUE (id_matricula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_id_matricula_fkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_id_matricula_fkey FOREIGN KEY (id_matricula) REFERENCES sga_secretaria.matriculas (id_matricula);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_id_estudiante_fkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_id_estudiante_fkey FOREIGN KEY (id_estudiante) REFERENCES sga_secretaria.estudiantes (id_estudiante);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_id_grado_origen_fkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_id_grado_origen_fkey FOREIGN KEY (id_grado_origen) REFERENCES sga_principal.grados (id_grado);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_id_ano_lectivo_fkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_id_ano_lectivo_fkey FOREIGN KEY (id_ano_lectivo) REFERENCES sga_principal.anos_lectivos (id_ano_lectivo);
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'hp_principal_registrado_por_fkey') THEN
        ALTER TABLE sga_principal.historial_promocion ADD CONSTRAINT hp_principal_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES sga_principal.usuarios (id_usuario);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_hp_principal_estudiante ON sga_principal.historial_promocion USING btree (id_estudiante, id_ano_lectivo);
CREATE INDEX IF NOT EXISTS idx_hp_principal_lamport_ts ON sga_principal.historial_promocion USING btree (lamport_ts);

-- =============================================================================
-- 2. sga_principal.eventos_academicos (calendario institucional compartido).
--    Estructura literal de microservicio-secretaria/.../db/migrations/006_eventos_academicos.sql
--    (el CRUD vive en CalendarioController de secretaria).
-- =============================================================================
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

-- =============================================================================
-- 3. sga_secretaria.grados y sga_secretaria.paralelos como vistas del catalogo
--    institucional (propiedad de sga-principal via gRPC; ver GradoService de
--    secretaria). Las usa IaSecretariaService.diagnosticoPorMatricula con
--    JOIN ... g.nombre ... p.nombre .... paralelos no tiene columna nombre:
--    se expone letra::text como nombre para compatibilidad.
-- =============================================================================
CREATE OR REPLACE VIEW sga_secretaria.grados AS
    SELECT * FROM sga_principal.grados;

CREATE OR REPLACE VIEW sga_secretaria.paralelos AS
    SELECT id_paralelo, id_grado, letra, letra::text AS nombre, activo
    FROM sga_principal.paralelos;

-- =============================================================================
-- 4. sga_soporte.historial_ticket (bitacora de cambios de ticket).
--    Estructura literal de microservicio-soporte/.../db/migrations/002_historial_ticket.sql
--    (la usa JdbcTicketRepository: id_historial, id_ticket, campo,
--    valor_anterior, valor_nuevo, modificado_por, fecha_modificacion).
-- =============================================================================
CREATE TABLE IF NOT EXISTS sga_soporte.historial_ticket (
    id_historial       BIGSERIAL PRIMARY KEY,
    id_ticket          BIGINT      NOT NULL REFERENCES sga_soporte.tickets (id_ticket) ON DELETE CASCADE,
    campo              VARCHAR(30) NOT NULL,   -- ESTADO, ASIGNADO_A, PRIORIDAD...
    valor_anterior     VARCHAR(100),
    valor_nuevo        VARCHAR(100),
    modificado_por     VARCHAR(50) NOT NULL,
    fecha_modificacion TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_historial_ticket ON sga_soporte.historial_ticket (id_ticket);

-- =============================================================================
-- 5. sga_principal.fn_horario_no_choque() + trigger tg_horario_no_choque.
--    Cuerpo literal de sga-principal/sql/V7__horarios.sql (fuera del camino de
--    Flyway): impide choque de docente, choque de curso y exceso de horas
--    semanales. Sin BEGIN/COMMIT: Flyway ya corre cada migracion en transaccion.
-- =============================================================================
CREATE OR REPLACE FUNCTION sga_principal.fn_horario_no_choque()
RETURNS TRIGGER AS $$
DECLARE
    v_docente BIGINT;
    v_grado   BIGINT;
    v_paralelo BIGINT;
    v_ano     BIGINT;
    v_choque_doc RECORD;
    v_choque_curso RECORD;
    v_asignadas INT;
    v_max_horas INT;
BEGIN
    -- Datos de la asignacion nueva
    SELECT a.id_docente, a.id_grado, a.id_paralelo, a.id_ano_lectivo, a.horas_semanales
      INTO v_docente, v_grado, v_paralelo, v_ano, v_max_horas
      FROM sga_principal.asignaciones a
     WHERE a.id_asignacion = NEW.id_asignacion;

    -- Choque de docente: mismo docente, mismo dia+periodo, otra asignacion.
    SELECT h.id_horario, a2.id_asignatura, a2.id_grado
      INTO v_choque_doc
      FROM sga_principal.horarios h
      JOIN sga_principal.asignaciones a2 ON a2.id_asignacion = h.id_asignacion
     WHERE a2.id_docente = v_docente
       AND h.dia_semana  = NEW.dia_semana
       AND h.id_periodo  = NEW.id_periodo
       AND h.id_horario <> COALESCE(NEW.id_horario, -1)
     LIMIT 1;
    IF v_choque_doc.id_horario IS NOT NULL THEN
        RAISE EXCEPTION 'CHOQUE DE DOCENTE: el docente ya tiene otra clase en ese dia y franja horaria.'
            USING ERRCODE = '23514';
    END IF;

    -- Choque de curso: mismo grado+paralelo+ano_lectivo, mismo dia+periodo, otra asignacion.
    SELECT h.id_horario
      INTO v_choque_curso
      FROM sga_principal.horarios h
      JOIN sga_principal.asignaciones a3 ON a3.id_asignacion = h.id_asignacion
     WHERE a3.id_grado         = v_grado
       AND a3.id_paralelo      = v_paralelo
       AND a3.id_ano_lectivo   = v_ano
       AND h.dia_semana        = NEW.dia_semana
       AND h.id_periodo        = NEW.id_periodo
       AND h.id_horario <> COALESCE(NEW.id_horario, -1)
     LIMIT 1;
    IF v_choque_curso.id_horario IS NOT NULL THEN
        RAISE EXCEPTION 'CHOQUE DE CURSO: el grado+paralelo ya tiene otra materia en ese dia y franja horaria.'
            USING ERRCODE = '23514';
    END IF;

    -- No superar las horas semanales del distributivo.
    SELECT COUNT(*) INTO v_asignadas
      FROM sga_principal.horarios
     WHERE id_asignacion = NEW.id_asignacion
       AND id_horario   <> COALESCE(NEW.id_horario, -1);
    IF (v_asignadas + 1) > v_max_horas THEN
        RAISE EXCEPTION 'EXCEDE HORAS: la asignacion ya cubre sus % horas semanales.', v_max_horas
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_horario_no_choque ON sga_principal.horarios;
CREATE TRIGGER tg_horario_no_choque
    BEFORE INSERT OR UPDATE ON sga_principal.horarios
    FOR EACH ROW EXECUTE FUNCTION sga_principal.fn_horario_no_choque();
