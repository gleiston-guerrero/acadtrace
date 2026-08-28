-- ============================================================
-- V7__horarios.sql -- Modulo de horarios escolares
-- Basado en la logica del sga_provincias (PHP) que ya funciona:
-- 6 franjas horarias de 07:30 a 12:30 (30h presenciales/semana).
-- ============================================================
BEGIN;

-- 1) Franjas horarias del dia (equivalente a "periodos" en el PHP viejo).
CREATE TABLE IF NOT EXISTS sga_principal.periodos_horario (
    id_periodo    SERIAL PRIMARY KEY,
    nombre        VARCHAR(30)  NOT NULL,
    hora_inicio   TIME         NOT NULL,
    hora_fin      TIME         NOT NULL,
    orden         INT          NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_periodo_orden UNIQUE (orden),
    CONSTRAINT ck_periodo_rango CHECK (hora_inicio < hora_fin)
);

INSERT INTO sga_principal.periodos_horario (nombre, hora_inicio, hora_fin, orden) VALUES
    ('1ra hora',  '07:30', '08:15', 1),
    ('2da hora',  '08:15', '09:00', 2),
    ('3ra hora',  '09:00', '09:45', 3),
    ('4ta hora',  '09:45', '10:30', 4),
    ('Receso',    '10:30', '11:00', 5),
    ('5ta hora',  '11:00', '11:45', 6),
    ('6ta hora',  '11:45', '12:30', 7)
ON CONFLICT (orden) DO NOTHING;

-- 2) Columna horas_semanales en asignaciones (30h presenciales por defecto).
--    Las 10h "en casa" NO se cuentan aqui.
ALTER TABLE sga_principal.asignaciones
    ADD COLUMN IF NOT EXISTS horas_semanales INT NOT NULL DEFAULT 4;

-- 3) Relacion horario -> periodos_horario (agregar id_periodo opcional).
ALTER TABLE sga_principal.horarios
    ADD COLUMN IF NOT EXISTS id_periodo INT
    REFERENCES sga_principal.periodos_horario(id_periodo) ON DELETE SET NULL;

-- 4) Restricciones a nivel BD (equivalente a las 4 validaciones del PHP):

-- 4a) Un mismo distributivo/asignacion no puede tener el mismo dia+franja duplicado.
ALTER TABLE sga_principal.horarios
    DROP CONSTRAINT IF EXISTS uq_horario_asignacion_dia_periodo;
ALTER TABLE sga_principal.horarios
    ADD  CONSTRAINT uq_horario_asignacion_dia_periodo
    UNIQUE (id_asignacion, dia_semana, id_periodo);

-- 4b) Choque de docente: un docente no puede estar en 2 lugares al mismo tiempo.
--     Se hace con indice unico expresado sobre docente_id derivado del join.
--     PostgreSQL no permite un UNIQUE cross-tabla directo, asi que lo modelamos
--     con una funcion de asercion mediante trigger.
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

COMMIT;
